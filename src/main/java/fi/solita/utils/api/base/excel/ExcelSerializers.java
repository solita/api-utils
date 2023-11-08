package fi.solita.utils.api.base.excel;

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
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
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
import fi.solita.utils.api.base.excel.ExcelSerializer.Cells;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
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

public class ExcelSerializers {
    
    private final Serializers s;
    
    public ExcelSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    
    
    /**
     * Some primitive serializers, to be used as helper functions for actual serialization
     */
    
    public static final <T> ExcelSerializer<T> stringSerializer(final Apply<T, ? extends CharSequence> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> charSerializer(final Apply<T, Character> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, Character.toString(f.apply(value))));
            }
        };
    }
    
    public static final <T> ExcelSerializer<T> shortSerializer(final Apply<T, Short> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> intSerializer(final Apply<T, Integer> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> longSerializer(final Apply<T, Long> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> bigIntegerSerializer(final Apply<T, BigInteger> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> bigDecimalSerializer(final Apply<T, BigDecimal> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    public static final <T> ExcelSerializer<T> booleanSerializer(final Apply<T, Boolean> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    
    public static final <T> ExcelSerializer<T> beanSerializer() {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
                if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                    return module.serialize(row, columnIndex, ((Enum<?>)value).name());
                } else {
                    List<Cell> cells = newMutableList();
                    List<String> headers = newMutableList();
                    StringBuilder sb = new StringBuilder();
                    for (Field f: sort(Compare.by(ExcelSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(value.getClass())))) {
                        Object val;
                        try {
                            val = f.get(value);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        
                        @SuppressWarnings("unchecked")
                        Class<Object> type = (Class<Object>) MemberUtil.memberTypeUnwrappingOptionAndEither(f);
                        String[] arr = newArray(String.class, repeat("", module.columns(type).size()));
                        Cells newCells = val == null || val instanceof Option && !((Option<?>)val).isDefined() ? module.serialize(row, columnIndex, Tuple.of((Object[])arr)) : module.serialize(row, columnIndex, val, type);
                        cells.addAll(newCells.cells);
                        
                        List<String> newHeaders = newCells.headers.isEmpty() ? module.columns(type) : newCells.headers;
                        newHeaders = newList(map(prepend(f.getName() + " ").andThen(ExcelSerializers_.trim), newHeaders));
                        headers.addAll(newHeaders);
                        
                        columnIndex += newCells.cells.size();
                        
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
            public List<String> columns(ExcelModule module, Class<T> type) {
                if (ClassUtils.getEnumType(type).isDefined()) {
                    return newList("");
                } else {
                    Iterable<Field> fields = sort(Compare.by(ExcelSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(type)));
                    return newList(flatMap(ExcelSerializers_.fieldColumn.ap(module), fields));
                }
            }
        };
    }
    
    static String fieldName(Field f) {
        return f.getName();
    }
    
    static String trim(String s) {
        return s.trim();
    }
    
    static List<String> fieldColumn(ExcelModule module, Field field) {
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
            StringBuilder sb = new StringBuilder();
            for (Cell c: cells.cells) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(CellFormat.getInstance("General").apply(c).text);
            }
            return sb.toString();
        }
    }
    
    protected static Cells merge(ExcelModule module, Row row, int columnIndex, Pair<?, ?> pair, String separator) {
        return merge(module, row, columnIndex, pair, append(separator));
    }
    
    protected static Cells merge(ExcelModule module, Row row, int columnIndex, Pair<?, ?> pair, Apply<? super String,String> mapFirst) {
        Cells alku = module.serialize(row, columnIndex, pair.left());
        Cells loppu = module.serialize(row, columnIndex+alku.cells.size(), pair.right());
        return new Cells(concat(alku.cells, loppu.cells), mapFirst.apply(cells2str(alku)) + cells2str(loppu)).withUnit(alku.unit);
    }
    
    protected static Cells merge(ExcelModule module, Row row, int columnIndex, Tuple3<?, ?, ?> tuple, Apply<? super String,String> mapFirst, Apply<? super String,String> mapSecond) {
        Cells a = module.serialize(row, columnIndex, tuple._1);
        Cells b = module.serialize(row, columnIndex+a.cells.size(), tuple._2);
        Cells c = module.serialize(row, columnIndex+a.cells.size()+b.cells.size(), tuple._3);
        return new Cells(concat(a.cells, b.cells, c.cells), mapFirst.apply(cells2str(a)) + mapSecond.apply(cells2str(b)) + cells2str(c)).withUnit(a.unit);
    }
    
    protected static Cell newCell(Row row, int columnIndex, CharSequence value) {
        Cell cell = row.createCell(columnIndex);
        if (value.length() >= 32767) {
            cell.setCellValue(value.toString().substring(0, 32700) + " ...(some content did not fit to cell max length!)");
        } else {
            cell.setCellValue(value.toString());
        }
        return cell;
    }
    
    protected static Cell newCell(Row row, int columnIndex, boolean value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        return cell;
    }
    
    protected static Cell newCell(Row row, int columnIndex, long value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        return cell;
    }
    
    protected static Cell newCell(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value.doubleValue());
        return cell;
    }
    
    protected static Cell newCell(Row row, int columnIndex, BigInteger value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value.doubleValue());
        return cell;
    }
    
    
    
    
    
    /**
     * Some concrete serializers for common types
     */
    
    private final ExcelSerializer<ResolvedMember> resolvedMember = new ExcelSerializer<ResolvedMember>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, ResolvedMember value) {
            throw new ResolvableMemberProvider.CannotResolveAsFormatException(SerializationFormat.XLSX);
        }
    };
    
    private final ExcelSerializer<?> nullValue = new ExcelSerializer<Object>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
            Cell cell = row.createCell(columnIndex);
            cell.setBlank();
            return new Cells(cell);
        }
    };
    
    private final ExcelSerializer<Map<?,?>> map_ = new ExcelSerializer<Map<?,?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Map<?, ?> value) {
            return new Cells(newCell(row, columnIndex, mkString("\r\n", map(ExcelModule_.serialize().ap(module, row, columnIndex).andThen(ExcelSerializers_.cells2str), value.entrySet()))));
        }
    };
    
    private final ExcelSerializer<?> array = new ExcelSerializer<Object>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
            List<Object> vals = newMutableList();
            for (int i = 0; i < Array.getLength(value); ++i) {
                vals.add(Array.get(value, i));
            }
            return module.serialize(row, columnIndex, vals);
        }
    };
    
    private final ExcelSerializer<Iterable<?>> iterable = new ExcelSerializer<Iterable<?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Iterable<?> value) {
            return new Cells(newCell(row, columnIndex, mkString("\r\n", map(ExcelModule_.serialize().ap(module, row, columnIndex).andThen(ExcelSerializers_.cells2str), value))));
        }
    };
    
    private final ExcelSerializer<Tuple> tuple = new ExcelSerializer<Tuple>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Tuple value) {
            List<Cell> cells = newMutableList();
            StringBuilder sb = new StringBuilder();
            for (Object v: value.toArray()) {
                Cells newCells = module.serialize(row, columnIndex, v);
                cells.addAll(newCells.cells);
                columnIndex += newCells.cells.size();
                
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(cells2str(newCells));
            }
            return new Cells(cells, "(" + sb.toString() + ")");
        }
        
        @Override
        public List<String> columns(ExcelModule module, Class<Tuple> type) {
            return newList(repeat("", type.getTypeParameters().length));
        }
    };
    
    private final ExcelSerializer<Map.Entry<?, ?>> entry = new ExcelSerializer<Map.Entry<?,?>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Map.Entry<?,?> value) {
            return ((ExcelSerializer<Pair<?,?>>)(Object)tuple).render(module, row, columnIndex, Pair.of(value.getKey(), value.getValue()));
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public List<String> columns(ExcelModule module, Class<Map.Entry<?,?>> type) {
            return ((ExcelSerializer<Map.Entry<?,?>>)(Object)tuple).columns(module, type);
        }
    };
    
    private final ExcelSerializer<Option<?>> option = new ExcelSerializer<Option<?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Option<?> value) {
            if (value.isDefined()) {
                return module.serialize(row, columnIndex, value.get());
            } else {
                Cell cell = row.createCell(columnIndex);
                cell.setBlank();
                return new Cells(cell);
            }
        }
        
        @Override
        public List<String> columns(ExcelModule module, Class<Option<?>> type) {
            throw new UnsupportedOperationException();
        }
    };
    
    private final ExcelSerializer<Either<?,?>> either = new ExcelSerializer<Either<?,?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Either<?, ?> value) {
            return module.serialize(row, columnIndex, value.isLeft() ? value.left.get() : value.right.get());
        }
    };
    
    private final ExcelSerializer<LocalDate> localdate = new ExcelSerializer<LocalDate>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, LocalDate value) {
            if (row.getSheet().getColumnStyle(columnIndex) == null || row.getSheet().getColumnStyle(columnIndex).getIndex() == 0) {
                CellStyle style = row.getSheet().getWorkbook().createCellStyle();
                style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd"));
                row.getSheet().setDefaultColumnStyle(columnIndex, style);
            }
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value.toDate());
            return new Cells(cell, value.toString());
        }
    };
    
    private final ExcelSerializer<LocalTime> localtime = new ExcelSerializer<LocalTime>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, LocalTime value) {
            if (row.getSheet().getColumnStyle(columnIndex) == null || row.getSheet().getColumnStyle(columnIndex).getIndex() == 0) {
                CellStyle style = row.getSheet().getWorkbook().createCellStyle();
                style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("hh:mm:ss"));
                row.getSheet().setDefaultColumnStyle(columnIndex, style);
            }
            Cell cell = row.createCell(columnIndex);
            // "Times are stored in Excel as decimals, between .0 and .99999, that represent a proportion of the day where .0 is 00:00:00 and .99999 is 23:59:59."
            cell.setCellValue(value.getMillisOfDay() / (24*60*60*1000) );
            return new Cells(cell, s.ser(value));
        }
    };
    
    private final ExcelSerializer<DateTime> datetime = new ExcelSerializer<DateTime>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, DateTime value) {
            if (row.getSheet().getColumnStyle(columnIndex) == null || row.getSheet().getColumnStyle(columnIndex).getIndex() == 0) {
                CellStyle style = row.getSheet().getWorkbook().createCellStyle();
                style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
                row.getSheet().setDefaultColumnStyle(columnIndex, style);
            }
            Cell cell = row.createCell(columnIndex);
            // uuh, menisiköhän näin kun excel ei tue aikavyöhykkeitä...
            cell.setCellValue(value.withZone(Serializers.APP_ZONE).toDate());
            return new Cells(cell, s.serZoned(value));
        }
    };
    
    private final ExcelSerializer<Interval> interval = new ExcelSerializer<Interval>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Interval value) {
            return merge(module, row, columnIndex, s.ser(value), " - ");
        }
        
        @Override
        public List<String> columns(ExcelModule module, Class<Interval> type) {
            return newList("alku", "loppu");
        }
    };
    
    
    
    
    
    public Map<Class<?>, ExcelSerializer<?>> serializers() { return newMap(
        Pair.of(ResolvedMember.class, resolvedMember),
        Pair.of(Option.class, option),
        Pair.of(Either.class, either),
        Pair.of(Tuple.class, tuple),
        Pair.of(URI.class, stringSerializer(Serializers_.ser.ap(s))),
        Pair.of(LocalDate.class, localdate),
        Pair.of(LocalTime.class, localtime),
        Pair.of(DateTime.class, datetime),
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
