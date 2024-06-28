package fi.solita.utils.api.base.html;

import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.zipWithIndex;
import static fi.solita.utils.functional.Predicates.not;
import static org.rendersnake.HtmlAttributesFactory.*;
import static org.rendersnake.HtmlAttributesFactory.src;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.Serializers_;
import fi.solita.utils.api.resolving.ResolvedMember;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyBiVoid;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.Tuple2;

public abstract class HtmlSerializers {
    
    private final Serializers s;
    
    public HtmlSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    
    
    /**
     * Some primitive serializers, to be used as helper functions for actual serialization
     */
    
    public static final <T> HtmlSerializer<T> serializer(final ApplyBiVoid<T,HtmlCanvas> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                f.accept(value, html);
            }
        };
    }
    
    public static final <T> HtmlSerializer<T> stringSerializer(final String cssTypeName, final Apply<T,? extends CharSequence> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value).toString())
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> charSerializer(final String cssTypeName, final Apply<T,Character> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value))
                    ._span();
            }
        };
    }
    
    public static final <T> HtmlSerializer<T> shortSerializer(final String cssTypeName, final Apply<T,Short> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write((int)f.apply(value))
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> intSerializer(final String cssTypeName, final Apply<T,Integer> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value))
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> longSerializer(final String cssTypeName, final Apply<T, Long> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(Long.toString(f.apply(value)))
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> doubleSerializer(final String cssTypeName, final Apply<T, Double> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(Double.toString(f.apply(value)))
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> bigIntegerSerializer(final String cssTypeName, final Apply<T, BigInteger> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value).toString())
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> bigDecimalSerializer(final String cssTypeName, final Apply<T, BigDecimal> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value).toPlainString())
                    ._span();
            }
        };
    }
    public static final <T> HtmlSerializer<T> booleanSerializer(final String cssTypeName, final Apply<T, Boolean> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                Boolean x = f.apply(value);
                html.span(class_("type-" + cssTypeName).title(x ? "true" : "false"))
                      .write(x ? "&#9989;" : "&#9940;", false)
                    ._span();
            }
        };
    }
    
    public static final <T> HtmlSerializer<T> delegateSerializer(final String cssTypeName, final Apply<T,?> f) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                        .render(module.toRenderable(f.apply(value)))
                    ._span();
            }
        };
    }
    
    /**
     * @param useValueAlsoForEnglish Defaults to false, in which case docName_en(value) is preferred for English.
     */
    public final <T extends Enum<T>> HtmlSerializer<T> enumSerializer(final String typeClassSuffix, final Apply<T,String> valueProducer, final boolean useValueAlsoForEnglish) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(lang("fi").class_("type-" + typeClassSuffix).title(docDescription(value).getOrElse(null)))
                        .write(valueProducer.apply(value))
                    ._span()
                    .span(lang("en").class_("type-" + typeClassSuffix).title(docDescription_en(value).getOrElse(null)))
                        .write(useValueAlsoForEnglish ? valueProducer.apply(value) : docName_en(value).getOrElse(valueProducer.apply(value)))
                    ._span();
            }
        };
    }
    
    public final <T extends Enum<T>> HtmlSerializer<T> enumSerializer(final String typeClassSuffix, final Apply<T,String> f) {
        return enumSerializer(typeClassSuffix, f, false);
    }
    
    public final <T> HtmlSerializer<T> beanSerializer() {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(final Object value, HtmlCanvas html, final HtmlModule module) throws IOException {
                if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                    html.span(lang("fi").title(docDescription((Enum<?>)value).getOrElse(((Enum<?>)value).name())))
                            .write(((Enum<?>)value).name())
                        ._span()
                        .span(lang("en").title(docName_en((Enum<?>)value).getOrElse(docDescription_en((Enum<?>)value).getOrElse(null))))
                            .write(((Enum<?>)value).name())
                        ._span();
                } else {
                    html.table()
                        .render(new Renderable() {
                          @Override
                          public void renderOn(HtmlCanvas html) throws IOException {
                            for (Field f: filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(value.getClass()))) {
                                Object val;
                                try {
                                    val = f.get(value);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                if (val != null) {
                                    html.tr()
                                          .th()
                                            .write(f.getName())
                                          ._th()
                                          .td()
                                            .render(module.toRenderable(val))
                                          ._td()
                                        ._tr();
                                }
                            }
                          }
                        })
                    ._table();
                }
            }
        };
    }
    
    protected abstract <T extends Enum<?>> Option<String> docName_en(T member);

    protected abstract <T extends Enum<?>> Option<String> docDescription_en(T member);

    protected abstract <T extends Enum<?>> Option<String> docDescription(T member);
    
    
    
    
    
    /**
     * Some concrete serializers for common types
     */
    
    private final HtmlSerializer<ResolvedMember> resolvedMember = new HtmlSerializer<ResolvedMember>() {
        @Override
        public void renderOn(ResolvedMember value, HtmlCanvas html, HtmlModule module) throws IOException {
            try {
                String content = new String(value.getData(), Charset.forName("UTF-8")).replaceFirst(".*<section\\s+id=\"content\">", "").replaceFirst("</section>.*", "");
                html.span(class_("type-resolved"))
                      .write(content, false)
                    ._span();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
    
    private final HtmlSerializer<?> nullValue = new HtmlSerializer<Object>() {
        @Override
        public void renderOn(Object value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("null"))
                ._span();
        }
    };
    
    // renders a Map as a nested table
    private final HtmlSerializer<Map<?,?>> map_ = new HtmlSerializer<Map<?,?>>() {
        @Override
        public void renderOn(final Map<?,?> value, HtmlCanvas html, final HtmlModule module) throws IOException {
            if (!value.isEmpty()) {
              html.table()
                    .render(new Renderable() {
                      @Override
                      public void renderOn(HtmlCanvas html) throws IOException {
                        for (Map.Entry<?, ?> e: value.entrySet()) {
                            html.tr()
                                  .th()
                                    .render(module.toRenderable(e.getKey()))
                                  ._th()
                                  .td()
                                    .render(module.toRenderable(e.getValue()))
                                  ._td()
                                ._tr();
                        }
                    }
                  })
                  ._table();
            }
        }
    };
    
    private final HtmlSerializer<?> array = new HtmlSerializer<Object>() {
        @Override
        public void renderOn(Object value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.ul();
            for (int i = 0; i < Array.getLength(value); ++i) {
                html.li(class_("i-" + i))
                      .render(module.toRenderable(Array.get(value, i)))
                    ._li();
            }
            html._ul();
        }
    };
    
    private final HtmlSerializer<Iterable<?>> iterable = new HtmlSerializer<Iterable<?>>() {
        @Override
        public void renderOn(Iterable<?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.ul();
            for (Tuple2<Integer, ?> o: zipWithIndex(value)) {
                html.li(class_("i-" + o._1))
                      .render(module.toRenderable(o._2))
                    ._li();
            }
            html._ul();
        }
    };
    
    private final HtmlSerializer<Tuple> tuple = new HtmlSerializer<Tuple>() {
        @SuppressWarnings("unchecked")
        @Override
        public void renderOn(Tuple value, HtmlCanvas html, HtmlModule module) throws IOException {
            ((HtmlSerializer<Object>)array).renderOn(value.toArray(), html, module);
        }
    };
    
    private final HtmlSerializer<Map.Entry<?,?>> entry = new HtmlSerializer<Map.Entry<?,?>>() {
        @Override
        public void renderOn(Map.Entry<?,?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.render(module.toRenderable(Pair.of(value)));
        }
    };
    
    private final HtmlSerializer<Option<?>> option = new HtmlSerializer<Option<?>>() {
        @Override
        public void renderOn(Option<?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            if (value.isDefined()) {
                html.render(module.toRenderable(value.get()));
            } else {
                html.span(class_("missing-value"))
                      .write("&nbsp;", HtmlCanvas.NO_ESCAPE)
                    ._span();
            }
        }
    };
    
    private final HtmlSerializer<Either<?,?>> either = new HtmlSerializer<Either<?,?>>() {
        @Override
        public void renderOn(Either<?,?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            if (value.isLeft()) {
                html.render(module.toRenderable(value.left.get()));
            } else {
                html.render(module.toRenderable(value.right.get()));
            }
        }
    };
    
    private final HtmlSerializer<URI> uri = new HtmlSerializer<URI>() {
        @Override
        public void renderOn(URI value, HtmlCanvas html, HtmlModule module) throws IOException {
            if (value.getPath().endsWith(".svg") || value.getPath().endsWith(".png") || value.getPath().endsWith(".gif") || value.getPath().endsWith(".jpg")) {
                html.span(class_("type-uri"))
                      .img(src(s.ser(value)).alt(s.ser(value)))
                    ._span();
            } else {
                html.span(class_("type-uri"))
                      .a(href(s.ser(value)))
                        .write(s.ser(value))
                      ._a()
                    ._span();
            }
        }
    };
    
    private final HtmlSerializer<UUID> uuid = new HtmlSerializer<UUID>() {
        @Override
        public void renderOn(UUID value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-uuid"))
                  .write(s.ser(value))
                ._span();
        }
    };
    
    private final HtmlSerializer<DateTime> datetime = new HtmlSerializer<DateTime>() {
        public final DateTime END_OF_ALL_TIMES = new DateTime(3000, 12, 31, 21, 59, DateTimeZone.UTC);
        public final DateTime START_OF_TIME = new DateTime(1921, 12, 31, 22, 0, DateTimeZone.UTC);
        
        public boolean isBeginOfTime(DateTime datetime) {
            return datetime.isBefore(START_OF_TIME.plusYears(1)); // some tolerace
        }

        public boolean isEndOfAllTimes(DateTime datetime) {
            return datetime.isAfter(END_OF_ALL_TIMES.minusYears(1)); // some tolerace
        }
        
        @Override
        public void renderOn(DateTime value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-datetime").title(isBeginOfTime(value) || isEndOfAllTimes(value) ? null : "UTC: " + s.ser(value)))
                  .write(isBeginOfTime(value) || isEndOfAllTimes(value) ? "∞" : s.serZoned(value))
                ._span();
        }
    };
    
    private final HtmlSerializer<Interval> interval = new HtmlSerializer<Interval>() {
        @Override
        public void renderOn(Interval value, HtmlCanvas html, HtmlModule module) throws IOException {
            Pair<DateTime,DateTime> i = s.ser(value);
            html.span(class_("type-interval"))
                  .render(module.toRenderable(i.left()))
                  .write(" - ")
                  .render(module.toRenderable(i.right()))
                ._span();
        }
    };
    
    
    
    
    
    public Map<Class<?>, HtmlSerializer<?>> serializers() { return newMap(
        Pair.of(ResolvedMember.class, resolvedMember),
        Pair.of(Option.class, option),
        Pair.of(Either.class, either),
        Pair.of(Tuple.class, tuple),
        Pair.of(URI.class, uri),
        Pair.of(UUID.class, uuid),
        Pair.of(LocalDate.class, stringSerializer("date", Serializers_.ser1.ap(s))),
        Pair.of(LocalTime.class, stringSerializer("time", Serializers_.ser2.ap(s))),
        Pair.of(DateTime.class, datetime),
        Pair.of(Interval.class, interval),
        Pair.of(Duration.class, stringSerializer("duration", Serializers_.ser5.ap(s))),
        Pair.of(Period.class, stringSerializer("period", Serializers_.ser6.ap(s))),
        Pair.of(DateTimeZone.class, stringSerializer("timezone", Serializers_.ser7.ap(s))),
        
        Pair.of(Map.Entry.class, entry),
        Pair.of(Boolean.class, booleanSerializer("boolean", Function.<Boolean>id())),
        Pair.of(CharSequence.class, stringSerializer("string", Function.<String>id())),
        Pair.of(Short.class, shortSerializer("integer", Function.<Short>id())),
        Pair.of(Integer.class, intSerializer("integer", Function.<Integer>id())),
        Pair.of(Long.class, longSerializer("integer", Function.<Long>id())),
        Pair.of(Double.class, doubleSerializer("double", Function.<Double>id())),
        Pair.of(BigDecimal.class, bigDecimalSerializer("decimal", Function.<BigDecimal>id())),
        Pair.of(BigInteger.class, bigIntegerSerializer("integer", Function.<BigInteger>id())),
        Pair.of(Character.class, charSerializer("char", Function.<Character>id())),
        Pair.of(Void.class, nullValue),
        Pair.of(JsonSerializeAsBean.class, beanSerializer()),
        Pair.of(Map.class, map_),
        Pair.of(Array.class, array),
        Pair.of(Iterable.class, iterable)
    );
    }
}
