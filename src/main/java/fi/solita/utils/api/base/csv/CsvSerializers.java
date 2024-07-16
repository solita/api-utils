package fi.solita.utils.api.base.csv;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.repeat;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Predicates.not;
import static fi.solita.utils.functional.Transformers.append;
import static fi.solita.utils.functional.Transformers.prepend;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.Serializers_;
import fi.solita.utils.api.base.csv.CsvSerializer.Cells;
import fi.solita.utils.api.resolving.ResolvedMember;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.Tuple3;

public class CsvSerializers {
    
    private final Serializers s;
    
    public CsvSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    
    
    /**
     * Some primitive serializers, to be used as helper functions for actual serialization
     */
    
    public static final <T> CsvSerializer<T> stringSerializer(final Apply<T,? extends CharSequence> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(f.apply(value));
            }
        };
    }
    public static final <T> CsvSerializer<T> charSerializer(final Apply<T,Character> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(Character.toString(f.apply(value)));
            }
        };
    }
    
    public static final <T> CsvSerializer<T> shortSerializer(final Apply<T, Short> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(Short.toString(f.apply(value)));
            }
        };
    }
    public static final <T> CsvSerializer<T> intSerializer(final Apply<T, Integer> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(Integer.toString(f.apply(value)));
            }
        };
    }
    public static final <T> CsvSerializer<T> longSerializer(final Apply<T, Long> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(Long.toString(f.apply(value)));
            }
        };
    }
    public static final <T> CsvSerializer<T> doubleSerializer(final Apply<T, Double> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(Double.toString(f.apply(value)));
            }
        };
    }
    public static final <T> CsvSerializer<T> bigIntegerSerializer(final Apply<T, BigInteger> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(f.apply(value).toString());
            }
        };
    }
    public static final <T> CsvSerializer<T> bigDecimalSerializer(final Apply<T, BigDecimal> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(f.apply(value).toPlainString());
            }
        };
    }
    public static final <T> CsvSerializer<T> booleanSerializer(final Apply<T, Boolean> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(f.apply(value) ? "kyllä" : "ei");
            }
        };
    }
    
    public static final <T> CsvSerializer<T> beanSerializer() {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, Object value) {
                if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                    return module.serialize(((Enum<?>)value).name());
                } else {
                    StringBuilder sb = new StringBuilder();
                    List<CharSequence> cells = newMutableList();
                    List<String> headers = newMutableList();
                    for (Field f: sort(Compare.by(CsvSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(value.getClass())))) {
                        Object val;
                        try {
                            val = f.get(value);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        
                        @SuppressWarnings("unchecked")
                        Class<Object> type = (Class<Object>) MemberUtil.memberTypeUnwrappingOptionAndEither(f);
                        String[] arr = newArray(String.class, repeat("", module.columns(type).size()));
                        Cells newCells = val == null || val instanceof Option && !((Option<?>)val).isDefined() ? module.serialize(Tuple.of((Object[])arr)) : module.serialize(val, type);
                        cells.addAll(newCells.cells);
    
                        List<String> newHeaders = newCells.headers.isEmpty() ? module.columns(type) : newCells.headers;
                        newHeaders = newList(map(prepend(f.getName() + " ").andThen(CsvSerializers_.trim), newHeaders));
                        headers.addAll(newHeaders);
                        
                        if (val != null && (!(val instanceof Option) || ((Option<?>)val).isDefined())) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(f.getName() + ": " + cells2str(newCells));
                        }
                    }
                    return new Cells(cells, sb.toString()).withHeaders(headers);
                }
            }
            @Override
            public List<String> columns(CsvModule module, Class<T> type) {
                if (ClassUtils.getEnumType(type).isDefined()) {
                    return newList("");
                } else {
                    Iterable<Field> fields = sort(Compare.by(CsvSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(type)));
                    return newList(flatMap(CsvSerializers_.fieldColumn.ap(module), fields));
                }
            }
        };
    }
    
    static String fieldName(Field f) {
        return f.getName();
    }
    
    static String trim(String str) {
        return str.trim();
    }
    
    static List<String> fieldColumn(CsvModule module, Field field) {
        List<String> ret = module.columns(MemberUtil.memberTypeUnwrappingOptionAndEither(field));
        return ret.isEmpty() || ret.equals(newList("")) ? newList(field.getName()) : ret;
    }
    
    
    
    
    
    /**
     * Some helper functions
     */
    
    public static String cells2str(Cells cells) {
        if (cells.stringRepresentation.isDefined()) {
            return cells.stringRepresentation.get().toString();
        } else {
            return mkString(" ", cells.cells);
        }
    }
    
    protected static Cells merge(CsvModule module, Pair<?, ?> pair, String separator) {
        return merge(module, pair, append(separator));
    }
    
    protected static Cells merge(CsvModule module, Pair<?, ?> pair, Apply<? super String,String> mapFirst) {
        Cells alku = module.serialize(pair.left());
        Cells loppu = module.serialize(pair.right());
        return new Cells(concat(alku.cells, loppu.cells), mapFirst.apply(cells2str(alku)) + cells2str(loppu)).withUnit(alku.unit);
    }
    
    protected static Cells merge(CsvModule module, Tuple3<?, ?, ?> tuple, Apply<? super String,String> mapFirst, Apply<? super String,String> mapSecond) {
        Cells a = module.serialize(tuple._1);
        Cells b = module.serialize(tuple._2);
        Cells c = module.serialize(tuple._3);
        return new Cells(concat(a.cells, b.cells, c.cells), mapFirst.apply(cells2str(a)) + mapSecond.apply(cells2str(b)) + cells2str(c)).withUnit(a.unit);
    }
    
    
    
    
    
    /**
     * Some concrete serializers for common types
     */
    
    private final CsvSerializer<ResolvedMember> resolvedMember = new CsvSerializer<ResolvedMember>() {
        @Override
        public Cells render(CsvModule module, ResolvedMember value) {
            return new Cells(new String(value.getData(), Charset.forName("UTF-8")));
        }
    };
    
    private final CsvSerializer<?> nullValue = new CsvSerializer<Object>() {
        @Override
        public Cells render(CsvModule module, Object value) {
            return new Cells("");
        }
    };
    
    private final CsvSerializer<Map<?,?>> map_ = new CsvSerializer<Map<?,?>>() {
        @Override
        public Cells render(CsvModule module, Map<?, ?> value) {
            return new Cells(mkString("\r\n", map(CsvModule_.serialize().ap(module).andThen(CsvSerializers_.cells2str), value.entrySet())));
        }
    };
    
    private final CsvSerializer<?> array = new CsvSerializer<Object>() {
        @Override
        public Cells render(CsvModule module, Object value) {
            List<Object> vals = newMutableList();
            for (int i = 0; i < Array.getLength(value); ++i) {
                vals.add(Array.get(value, i));
            }
            return module.serialize(vals);
        }
    };
    
    private final CsvSerializer<Iterable<?>> iterable = new CsvSerializer<Iterable<?>>() {
        @Override
        public Cells render(CsvModule module, Iterable<?> value) {
            return new Cells(mkString("\r\n", map(CsvModule_.serialize().ap(module).andThen(CsvSerializers_.cells2str), value)));
        }
    };
    
    private final CsvSerializer<Tuple> tuple = new CsvSerializer<Tuple>() {
        @Override
        public Cells render(CsvModule module, Tuple value) {
            List<CharSequence> cells = newMutableList();
            StringBuilder sb = new StringBuilder();
            for (Object v: value.toArray()) {
                Cells newCells = module.serialize(v);
                cells.addAll(newCells.cells);
                
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(cells2str(newCells));
            }
            return new Cells(cells, "(" + sb.toString() + ")");
        }
        
        @Override
        public List<String> columns(CsvModule module, Class<Tuple> type) {
            return newList(repeat("", type.getTypeParameters().length));
        }
    };
    
    private final CsvSerializer<Map.Entry<?,?>> entry = new CsvSerializer<Map.Entry<?,?>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Cells render(CsvModule module, Map.Entry<?,?> value) {
            return ((CsvSerializer<Pair<?,?>>)(Object)tuple).render(module, Pair.of(value.getKey(), value.getValue()));
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public List<String> columns(CsvModule module, Class<Map.Entry<?,?>> type) {
            return ((CsvSerializer<Map.Entry<?,?>>)(Object)tuple).columns(module, type);
        }
    };
    
    private final CsvSerializer<Option<?>> option = new CsvSerializer<Option<?>>() {
        @Override
        public Cells render(CsvModule module, Option<?> value) {
            if (value.isDefined()) {
                return module.serialize(value.get());
            } else {
                return new Cells("");
            }
        }
        
        @Override
        public List<String> columns(CsvModule module, Class<Option<?>> type) {
            throw new UnsupportedOperationException();
        }
    };
    
    private final CsvSerializer<Either<?,?>> either = new CsvSerializer<Either<?,?>>() {
        @Override
        public Cells render(CsvModule module, Either<?, ?> value) {
            return module.serialize(value.isLeft() ? value.left.get() : value.right.get());
        }
    };
    
    private final CsvSerializer<Interval> interval = new CsvSerializer<Interval>() {
        @Override
        public Cells render(CsvModule module, Interval value) {
            return merge(module, s.ser(value), " - ");
        }
        
        @Override
        public List<String> columns(CsvModule module, Class<Interval> type) {
            return newList("alku", "loppu");
        }
    };
    
    
    
    
    
    public Map<Class<?>, CsvSerializer<?>> serializers() { return newMap(
        Pair.of(ResolvedMember.class, resolvedMember),
        Pair.of(Option.class, option),
        Pair.of(Either.class, either),
        Pair.of(Tuple.class, tuple),
        Pair.of(URI.class, stringSerializer(Serializers_.ser.ap(s))),
        Pair.of(UUID.class, stringSerializer(Serializers_.ser9.ap(s))),
        Pair.of(LocalDate.class, stringSerializer(Serializers_.ser1.ap(s))),
        Pair.of(LocalTime.class, stringSerializer(Serializers_.ser2.ap(s))),
        Pair.of(DateTime.class, stringSerializer(Serializers_.serZoned.ap(s))),
        Pair.of(Interval.class, interval),
        Pair.of(Duration.class, stringSerializer(Serializers_.ser5.ap(s))),
        Pair.of(Period.class, stringSerializer(Serializers_.ser6.ap(s))),
        Pair.of(DateTimeZone.class, stringSerializer(Serializers_.ser7.ap(s))),
        
        Pair.of(Map.Entry.class, entry),
        Pair.of(Boolean.class, booleanSerializer(Function.<Boolean>id())),
        Pair.of(CharSequence.class, stringSerializer(Function.<CharSequence>id())),
        Pair.of(Short.class, shortSerializer(Function.<Short>id())),
        Pair.of(Integer.class, intSerializer(Function.<Integer>id())),
        Pair.of(Long.class, longSerializer(Function.<Long>id())),
        Pair.of(Double.class, doubleSerializer(Function.<Double>id())),
        Pair.of(BigDecimal.class, bigDecimalSerializer(Function.<BigDecimal>id())),
        Pair.of(BigInteger.class, bigIntegerSerializer(Function.<BigInteger>id())),
        Pair.of(Character.class, charSerializer(Function.<Character>id())),
        Pair.of(Void.class, nullValue),
        Pair.of(JsonSerializeAsBean.class, beanSerializer()),
        Pair.of(Map.class, map_),
        Pair.of(Array.class, array),
        Pair.of(Iterable.class, iterable)
    );
    }
}
