package fi.solita.utils.api.base;

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

import fi.solita.utils.api.ClassUtils;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.MemberUtil;
import fi.solita.utils.api.base.ExcelSerializer.Cells;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
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
    
    public static final <T> ExcelSerializer<T> stdSerializer(final Apply<T, ? extends CharSequence> f) {
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return new Cells(newCell(row, columnIndex, f.apply(value)));
            }
        };
    }
    
    public static String cells2str(Cells cells) {
        if (cells.stringRepresentation.isDefined()) {
            return cells.stringRepresentation.get().toString();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Cell c: cells.cells) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(CellFormat.GENERAL_FORMAT.apply(c).text);
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
        Cells b = module.serialize(row, columnIndex+1, tuple._2);
        Cells c = module.serialize(row, columnIndex+2, tuple._3);
        return new Cells(concat(a.cells, b.cells, c.cells), mapFirst.apply(cells2str(a)) + mapSecond.apply(cells2str(b)) + cells2str(c)).withUnit(a.unit);
    }
    
    protected static Cell newCell(Row row, int columnIndex, CharSequence value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value.toString());
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
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> nullValue = Pair.of(Void.class, new ExcelSerializer<Object>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellType(Cell.CELL_TYPE_BLANK);
            return new Cells(cell);
        }
    });
    
    static String fieldName(Field f) {
        return f.getName();
    }
    
    static String trim(String s) {
        return s.trim();
    }
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> jsonOutput = Pair.of(JsonSerializeAsBean.class, new ExcelSerializer<Object>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
            if (ClassUtils.getEnumType(value.getClass()).isDefined()) {
                return module.serialize(row, columnIndex, ((Enum<?>)value).name());
            } else {
                List<Cell> cells = newList();
                List<String> headers = newList();
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
        public List<String> columns(ExcelModule module, Class<Object> type) {
            if (ClassUtils.getEnumType(type).isDefined()) {
                return newList("");
            } else {
                Iterable<Field> fields = sort(Compare.by(ExcelSerializers_.fieldName), filter(Predicate.of(ClassUtils.PublicMembers).and(not(ClassUtils.StaticMembers)), ClassUtils.AllDeclaredApplicationFields.apply(type)));
                return newList(flatMap(ExcelSerializers_.fieldColumn.ap(module), fields));
            }
        }
    });
    
    static List<String> fieldColumn(ExcelModule module, Field field) {
        List<String> ret = module.columns(MemberUtil.memberTypeUnwrappingOptionAndEither(field));
        return ret.isEmpty() || ret.equals(newList("")) ? newList(field.getName()) : ret;
    }
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> map_ = Pair.of(Map.class, new ExcelSerializer<Map<?,?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Map<?, ?> value) {
            return new Cells(newCell(row, columnIndex, mkString("\r\n", map(ExcelModule_.serialize().ap(module, row, columnIndex).andThen(ExcelSerializers_.cells2str), value.entrySet()))));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> array = Pair.of(Array.class, new ExcelSerializer<Object>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Object value) {
            List<Object> vals = newList();
            for (int i = 0; i < Array.getLength(value); ++i) {
                vals.add(Array.get(value, i));
            }
            return module.serialize(row, columnIndex, vals);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> iterable = Pair.of(Iterable.class, new ExcelSerializer<Iterable<?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Iterable<?> value) {
            return new Cells(newCell(row, columnIndex, mkString("\r\n", map(ExcelModule_.serialize().ap(module, row, columnIndex).andThen(ExcelSerializers_.cells2str), value))));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> tuple = Pair.of(Tuple.class, new ExcelSerializer<Tuple>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Tuple value) {
            List<Cell> cells = newList();
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
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> entry = Pair.of(Map.Entry.class, new ExcelSerializer<Map.Entry<?,?>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Map.Entry<?,?> value) {
            return ((ExcelSerializer<Pair<?,?>>)tuple.getValue()).render(module, row, columnIndex, Pair.of(value.getKey(), value.getValue()));
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public List<String> columns(ExcelModule module, Class<Map.Entry<?,?>> type) {
            return ((ExcelSerializer<Map.Entry<?,?>>)tuple.getValue()).columns(module, type);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> bool = Pair.of(Boolean.class, new ExcelSerializer<Boolean>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Boolean value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> charSequence = Pair.of(CharSequence.class, new ExcelSerializer<CharSequence>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, CharSequence value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> shortInteger = Pair.of(Short.class, new ExcelSerializer<Short>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Short value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> integer = Pair.of(Integer.class, new ExcelSerializer<Integer>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Integer value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });

    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> longInteger = Pair.of(Long.class, new ExcelSerializer<Long>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Long value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> bigDecimal = Pair.of(BigDecimal.class, new ExcelSerializer<BigDecimal>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, BigDecimal value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> bigInteger = Pair.of(BigInteger.class, new ExcelSerializer<BigInteger>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, BigInteger value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> character = Pair.of(Character.class, new ExcelSerializer<Character>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Character value) {
            return new Cells(newCell(row, columnIndex, value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> option = Pair.of(Option.class, new ExcelSerializer<Option<?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Option<?> value) {
            if (value.isDefined()) {
                return module.serialize(row, columnIndex, value.get());
            } else {
                Cell cell = row.createCell(columnIndex);
                cell.setCellType(Cell.CELL_TYPE_BLANK);
                return new Cells(cell);
            }
        }
        
        @Override
        public List<String> columns(ExcelModule module, Class<Option<?>> type) {
            throw new UnsupportedOperationException();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> either = Pair.of(Either.class, new ExcelSerializer<Either<?,?>>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Either<?, ?> value) {
            return module.serialize(row, columnIndex, value.isLeft() ? value.left.get() : value.right.get());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> localdate = Pair.of(LocalDate.class, new ExcelSerializer<LocalDate>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, LocalDate value) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value.toDate());
            CellStyle style = row.getSheet().getWorkbook().createCellStyle();
            style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd"));
            cell.setCellStyle(style);
            return new Cells(cell, value.toString());
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> localtime = Pair.of(LocalTime.class, new ExcelSerializer<LocalTime>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, LocalTime value) {
            Cell cell = row.createCell(columnIndex);
            // "Times are stored in Excel as decimals, between .0 and .99999, that represent a proportion of the day where .0 is 00:00:00 and .99999 is 23:59:59."
            cell.setCellValue(value.getMillisOfDay() / (24*60*60*1000) );
            CellStyle style = row.getSheet().getWorkbook().createCellStyle();
            style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("hh:mm:ss"));
            cell.setCellStyle(style);
            return new Cells(cell, s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> datetime = Pair.of(DateTime.class, new ExcelSerializer<DateTime>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, DateTime value) {
            Cell cell = row.createCell(columnIndex);
            // uuh, menisiköhän näin kun excel ei tuo aikavyöhykkeitä...
            cell.setCellValue(value.withZone(Serializers.APP_ZONE).toDate());
            CellStyle style = row.getSheet().getWorkbook().createCellStyle();
            style.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            cell.setCellStyle(style);
            return new Cells(cell, s.serZoned(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends ExcelSerializer<?>> interval = Pair.of(Interval.class, new ExcelSerializer<Interval>() {
        @Override
        public Cells render(ExcelModule module, Row row, int columnIndex, Interval value) {
            return merge(module, row, columnIndex, s.ser(value), " - ");
        }
        
        @Override
        public List<String> columns(ExcelModule module, Class<Interval> type) {
            return newList("alku", "loppu");
        }
    });
    
    public Map<Class<?>, ExcelSerializer<?>> serializers() { return newMap(
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
            localdate,
            localtime,
            datetime,
            interval,
            Pair.of(Duration.class, stdSerializer(Serializers_.ser5.ap(s))),
            Pair.of(DateTimeZone.class, stdSerializer(Serializers_.ser6.ap(s)))
        );
    }
}
