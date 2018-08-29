package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.zipWithIndex;
import static fi.solita.utils.functional.Predicates.not;
import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;
import static org.rendersnake.HtmlAttributesFactory.src;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fi.solita.utils.api.ClassUtils;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.ResolvedMember;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function2;
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
    
    public static final <T> HtmlSerializer<T> stringSerializer(final Apply<T,String> f, final String cssTypeName) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value))
                    ._span();
            }
        };
    }
    
    public static final <T> HtmlSerializer<T> intSerializer(final Apply<T,Integer> f, final String cssTypeName) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                      .write(f.apply(value))
                    ._span();
            }
        };
    }
    
    public static final <T> HtmlSerializer<T> delegateSerializer(final Apply<T,?> f, final String cssTypeName) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("type-" + cssTypeName))
                        .render(module.toRenderable(f.apply(value)))
                    ._span();
            }
        };
    }
    
    protected abstract <T extends Enum<?>> Option<String> docName_en(T member);

    protected abstract <T extends Enum<?>> Option<String> docDescription_en(T member);

    protected abstract <T extends Enum<?>> Option<String> docDescription(T member);
    
    public final <T extends Enum<T>, S extends Serializers> HtmlSerializer<T> enumSerializer(final S s, final String typeClassSuffix, final Function2<S,T,String> serializer) {
        return new HtmlSerializer<T>() {
            @Override
            public void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException {
                html.span(class_("fi type-" + typeClassSuffix).title(docDescription(value).getOrElse(null)))
                        .write(serializer.ap(s).apply(value))
                    ._span()
                    .span(class_("en type-" + typeClassSuffix).title(docDescription_en(value).getOrElse(docName_en(value).getOrElse(null))))
                        .write(serializer.ap(s).apply(value))
                    ._span();
            }
        };
    }
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> resolvedMember = Pair.of(ResolvedMember.class, new HtmlSerializer<ResolvedMember>() {
        @Override
        public void renderOn(ResolvedMember value, HtmlCanvas html, HtmlModule module) throws IOException {
            try {
                String content = value.getData().replaceFirst(".*<section\\s+id=\"content\">", "").replaceFirst("</section>.*", "");
                html.span(class_("type-resolved"))
                      .write(content, false)
                    ._span();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> nullValue = Pair.of(Void.class, new HtmlSerializer<Object>() {
        @Override
        public void renderOn(Object value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("null"))
                ._span();
        }
    });
    
    // for @JsonOutputs serialize all public instance fields as a table
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> jsonOutput = Pair.of(JsonSerializeAsBean.class, new HtmlSerializer<Object>() {
        @Override
        public void renderOn(final Object value, HtmlCanvas html, final HtmlModule module) throws IOException {
            if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                html.span(class_("fi").title(docDescription((Enum<?>)value).getOrElse(((Enum<?>)value).name())))
                        .write(((Enum<?>)value).name())
                    ._span()
                    .span(class_("en").title(docName_en((Enum<?>)value).getOrElse(docDescription_en((Enum<?>)value).getOrElse(null))))
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
    });
    
    // render a Map as a nested table
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> map = Pair.of(Map.class, new HtmlSerializer<Map<?,?>>() {
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> array = Pair.of(Array.class, new HtmlSerializer<Object>() {
        @Override
        public void renderOn(Object value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.ul();
            for (int i = 0; i < Array.getLength(value); ++i) {
                html.li(class_("index-" + i))
                      .render(module.toRenderable(Array.get(value, i)))
                    ._li();
            }
            html._ul();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> iterable = Pair.of(Iterable.class, new HtmlSerializer<Iterable<?>>() {
        @Override
        public void renderOn(Iterable<?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.ul();
            for (Tuple2<Integer, ?> o: zipWithIndex(value)) {
                html.li(class_("index-" + o._1))
                      .render(module.toRenderable(o._2))
                    ._li();
            }
            html._ul();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> tuple = Pair.of(Tuple.class, new HtmlSerializer<Tuple>() {
        @SuppressWarnings("unchecked")
        @Override
        public void renderOn(Tuple value, HtmlCanvas html, HtmlModule module) throws IOException {
            ((HtmlSerializer<Object>)array.getValue()).renderOn(value.toArray(), html, module);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> bool = Pair.of(Boolean.class, new HtmlSerializer<Boolean>() {
        @Override
        public void renderOn(Boolean value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-boolean").title(value ? "true" : "false"))
                  .write(value ? "&#9989;" : "&#9940;", false)
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> string = Pair.of(String.class, new HtmlSerializer<String>() {
        @Override
        public void renderOn(String value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-string"))
                  .write(value)
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> shortInteger = Pair.of(Short.class, new HtmlSerializer<Short>() {
        @Override
        public void renderOn(Short value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-integer"))
                  .write(Short.toString(value))
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> integer = Pair.of(Integer.class, new HtmlSerializer<Integer>() {
        @Override
        public void renderOn(Integer value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-integer"))
                  .write(value)
                ._span();
        }
    });

    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> longInteger = Pair.of(Long.class, new HtmlSerializer<Long>() {
        @Override
        public void renderOn(Long value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-integer"))
                  .write(Long.toString(value))
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> bigDecimal = Pair.of(BigDecimal.class, new HtmlSerializer<BigDecimal>() {
        @Override
        public void renderOn(BigDecimal value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-decimal"))
                  .write(value.toPlainString())
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> bigInteger = Pair.of(BigInteger.class, new HtmlSerializer<BigInteger>() {
        @Override
        public void renderOn(BigInteger value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-integer"))
                  .write(value.toString())
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> character = Pair.of(Character.class, new HtmlSerializer<Character>() {
        @Override
        public void renderOn(Character value, HtmlCanvas html, HtmlModule module) throws IOException {
            html.span(class_("type-char"))
                  .write(value)
                ._span();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> option = Pair.of(Option.class, new HtmlSerializer<Option<?>>() {
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> either = Pair.of(Either.class, new HtmlSerializer<Either<?,?>>() {
        @Override
        public void renderOn(Either<?,?> value, HtmlCanvas html, HtmlModule module) throws IOException {
            if (value.isLeft()) {
                html.render(module.toRenderable(value.left.get()));
            } else {
                html.render(module.toRenderable(value.right.get()));
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> uri = Pair.of(URI.class, new HtmlSerializer<URI>() {
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> datetime = Pair.of(DateTime.class, new HtmlSerializer<DateTime>() {
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends HtmlSerializer<?>> interval = Pair.of(Interval.class, new HtmlSerializer<Interval>() {
        @Override
        public void renderOn(Interval value, HtmlCanvas html, HtmlModule module) throws IOException {
            Pair<DateTime,DateTime> i = s.ser(value);
            html.span(class_("type-interval"))
                  .render(module.toRenderable(i.left()))
                  .write(" - ")
                  .render(module.toRenderable(i.right()))
                ._span();
        }
    });
    
    public Map<Class<?>, HtmlSerializer<?>> serializers() { return newMap(
            resolvedMember,
            nullValue,
            jsonOutput,
            map,
            array,
            iterable,
            tuple,
            bool,
            string,
            shortInteger,
            integer,
            longInteger,
            bigDecimal,
            bigInteger,
            character,
            option,
            either,
            uri,
            Pair.of(LocalDate.class, stringSerializer(Serializers_.ser1.ap(s), "date")),
            Pair.of(LocalTime.class, stringSerializer(Serializers_.ser2.ap(s), "time")),
            datetime,
            interval,
            Pair.of(Duration.class, stringSerializer(Serializers_.ser5.ap(s), "duration")),
            Pair.of(DateTimeZone.class, stringSerializer(Serializers_.ser6.ap(s), "timezone"))
        );
    }
}
