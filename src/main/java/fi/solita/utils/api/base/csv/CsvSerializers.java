package fi.solita.utils.api.base.csv;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.csv.CsvModule_;
import fi.solita.utils.api.base.csv.CsvSerializers_;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.Serializers_;
import fi.solita.utils.api.base.csv.CsvSerializer.Cells;
import fi.solita.utils.api.resolving.ResolvedMember;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
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
    
    public static final <T> CsvSerializer<T> stdSerializer(final Apply<T,? extends CharSequence> f) {
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return new Cells(f.apply(value));
            }
        };
    }
    
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
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> resolvedMember = Pair.of(ResolvedMember.class, new CsvSerializer<ResolvedMember>() {
        @Override
        public Cells render(CsvModule module, ResolvedMember value) {
            return new Cells(new String(value.getData(), Charset.forName("UTF-8")));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> nullValue = Pair.of(Void.class, new CsvSerializer<Object>() {
        @Override
        public Cells render(CsvModule module, Object value) {
            return new Cells("");
        }
    });
    
    static String fieldName(Field f) {
        return f.getName();
    }
    
    static String trim(String str) {
        return str.trim();
    }
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> jsonOutput = Pair.of(JsonSerializeAsBean.class, new CsvSerializer<Object>() {
        @Override
        public Cells render(CsvModule module, Object value) {
            if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                return module.serialize(((Enum<?>)value).name());
            } else {
                StringBuilder sb = new StringBuilder();
                List<CharSequence> cells = newList();
                List<String> headers = newList();
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
        public List<String> columns(CsvModule module, Class<Object> type) {
            if (ClassUtils.getEnumType(type).isDefined()) {
                return newList("");
            } else {
                Iterable<Field> fields = sort(Compare.by(CsvSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(type)));
                return newList(flatMap(CsvSerializers_.fieldColumn.ap(module), fields));
            }
        }
    });
    
    static List<String> fieldColumn(CsvModule module, Field field) {
        List<String> ret = module.columns(MemberUtil.memberTypeUnwrappingOptionAndEither(field));
        return ret.isEmpty() || ret.equals(newList("")) ? newList(field.getName()) : ret;
    }
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> map_ = Pair.of(Map.class, new CsvSerializer<Map<?,?>>() {
        @Override
        public Cells render(CsvModule module, Map<?, ?> value) {
            return new Cells(mkString("\r\n", map(CsvModule_.serialize().ap(module).andThen(CsvSerializers_.cells2str), value.entrySet())));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> array = Pair.of(Array.class, new CsvSerializer<Object>() {
        @Override
        public Cells render(CsvModule module, Object value) {
            List<Object> vals = newList();
            for (int i = 0; i < Array.getLength(value); ++i) {
                vals.add(Array.get(value, i));
            }
            return module.serialize(vals);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> iterable = Pair.of(Iterable.class, new CsvSerializer<Iterable<?>>() {
        @Override
        public Cells render(CsvModule module, Iterable<?> value) {
            return new Cells(mkString("\r\n", map(CsvModule_.serialize().ap(module).andThen(CsvSerializers_.cells2str), value)));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> tuple = Pair.of(Tuple.class, new CsvSerializer<Tuple>() {
        @Override
        public Cells render(CsvModule module, Tuple value) {
            List<CharSequence> cells = newList();
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> entry = Pair.of(Map.Entry.class, new CsvSerializer<Map.Entry<?,?>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Cells render(CsvModule module, Map.Entry<?,?> value) {
            return ((CsvSerializer<Pair<?,?>>)tuple.getValue()).render(module, Pair.of(value.getKey(), value.getValue()));
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public List<String> columns(CsvModule module, Class<Map.Entry<?,?>> type) {
            return ((CsvSerializer<Map.Entry<?,?>>)tuple.getValue()).columns(module, type);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> bool = Pair.of(Boolean.class, new CsvSerializer<Boolean>() {
        @Override
        public Cells render(CsvModule module, Boolean value) {
            return new Cells(value ? "kyllä" : "ei");
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> charSequence = Pair.of(CharSequence.class, new CsvSerializer<CharSequence>() {
        @Override
        public Cells render(CsvModule module, CharSequence value) {
            return new Cells(value.toString());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> shortInteger = Pair.of(Short.class, new CsvSerializer<Short>() {
        @Override
        public Cells render(CsvModule module, Short value) {
            return new Cells(Short.toString(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> integer = Pair.of(Integer.class, new CsvSerializer<Integer>() {
        @Override
        public Cells render(CsvModule module, Integer value) {
            return new Cells(Integer.toString(value));
        }
    });

    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> longInteger = Pair.of(Long.class, new CsvSerializer<Long>() {
        @Override
        public Cells render(CsvModule module, Long value) {
            return new Cells(Long.toString(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> bigDecimal = Pair.of(BigDecimal.class, new CsvSerializer<BigDecimal>() {
        @Override
        public Cells render(CsvModule module, BigDecimal value) {
            return new Cells(value.toPlainString());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> bigInteger = Pair.of(BigInteger.class, new CsvSerializer<BigInteger>() {
        @Override
        public Cells render(CsvModule module, BigInteger value) {
            return new Cells(value.toString());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> character = Pair.of(Character.class, new CsvSerializer<Character>() {
        @Override
        public Cells render(CsvModule module, Character value) {
            return new Cells(Character.toString(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> option = Pair.of(Option.class, new CsvSerializer<Option<?>>() {
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> either = Pair.of(Either.class, new CsvSerializer<Either<?,?>>() {
        @Override
        public Cells render(CsvModule module, Either<?, ?> value) {
            return module.serialize(value.isLeft() ? value.left.get() : value.right.get());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends CsvSerializer<?>> interval = Pair.of(Interval.class, new CsvSerializer<Interval>() {
        @Override
        public Cells render(CsvModule module, Interval value) {
            return merge(module, s.ser(value), " - ");
        }
        
        @Override
        public List<String> columns(CsvModule module, Class<Interval> type) {
            return newList("alku", "loppu");
        }
    });
    
    public Map<Class<?>, CsvSerializer<?>> serializers() { return newMap(
            resolvedMember,
            nullValue,
            jsonOutput,
            map_,
            array,
            iterable,
            tuple,
            entry,
            bool,
            charSequence,
            shortInteger,
            integer,
            longInteger,
            bigDecimal,
            bigInteger,
            character,
            option,
            either,
            Pair.of(URI.class, stdSerializer(Serializers_.ser.ap(s))),
            Pair.of(LocalDate.class, stdSerializer(Serializers_.ser1.ap(s))),
            Pair.of(LocalTime.class, stdSerializer(Serializers_.ser2.ap(s))),
            Pair.of(DateTime.class, stdSerializer(Serializers_.serZoned.ap(s))),
            interval,
            Pair.of(Duration.class, stdSerializer(Serializers_.ser5.ap(s))),
            Pair.of(DateTimeZone.class, stdSerializer(Serializers_.ser6.ap(s)))
        );
    }
}
