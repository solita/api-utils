package fi.solita.utils.api.base.excel;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.FunctionalS.range;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.csv.CsvSerializer;
import fi.solita.utils.api.base.excel.ExcelSerializer.Cells;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Tuple;

public class ExcelModule {

    public final Map<Class<?>, ExcelSerializer<?>> serializers;

    public ExcelModule(Map<Class<?>, ExcelSerializer<?>> serializers) {
        this.serializers = serializers;
    }
    
    @SuppressWarnings("unchecked")
    public <T> Cells serialize(Row row, int columnIndex, T obj) {
        Assert.notNull(obj);
        return serialize(row, columnIndex, obj, obj instanceof Option && ((Option<?>)obj).isDefined() ? ((Option<?>)obj).get().getClass() : (Class<T>)obj.getClass());
    }

    public <T> Cells serialize(Row row, int columnIndex, T obj, Class<?> type) {
        if (obj == null || !Option.class.isAssignableFrom(type) && obj instanceof Option && !((Option<?>)obj).isDefined()) {
            List<Cell> cells = newMutableList();
            List<String> cols = columns(type);
            for (int index: range(0, cols.size()-1)) {
                Cell cell = row.createCell(columnIndex + index);
                cell.setCellValue("");
                cells.add(cell);
            }
            return new Cells(cells, "").withHeaders(cols);
        }
        
        if (!Option.class.isAssignableFrom(type) && obj instanceof Option && ((Option<?>)obj).isDefined()) {
            return serialize(row, columnIndex, ((Option<?>)obj).get(), type);
        }
        
        for (ExcelSerializer<Object> ser: resolveSerializer(obj.getClass())) {
            Cells ret = ser.render(this, row, columnIndex, obj);
            if (obj instanceof Option) {
                if (Option.class.isAssignableFrom(type)) {
                    return ret.withHeaders(ret.headers.isEmpty() ? newList("") : ret.headers);
                } else {
                    List<String> cols = ret.headers.isEmpty() ? columns(type) : ret.headers;
                    return ret.withHeaders(cols);
                }
            } else if (obj instanceof Tuple) {
                return ret;
            } else if (obj instanceof Map.Entry<?,?>) {
                return ret;
            } else if (obj instanceof Iterable) {
                Assert.equal(1, ret.cells.size());
                return ret.withHeaders(ret.headers.isEmpty() ? newList("") : ret.headers);
            } else {
                List<String> cols = ret.headers.isEmpty() ? columns(type) : ret.headers;
                Assert.equal(cols.size(), ret.cells.size(), "Wrong number of columns (expected " + cols.size() + ", was " + ret.cells.size() + ") while serializing: " + type);
                return ret.withHeaders(cols);
            }
        }
        throw new RuntimeException("No Excel serializer for type: " + obj.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private <T> Option<ExcelSerializer<T>> resolveSerializer(Class<?> type) {
        ExcelSerializer<?> ret = serializers.get(type);
        if (ret != null) {
            return Some((ExcelSerializer<T>)ret);
        }
        
        // try direct interface implementations
        for (Class<?> e: type.getInterfaces()) {
            for (ExcelSerializer<?> excelSerializer: find(e, serializers)) {
                return Some((ExcelSerializer<T>)excelSerializer);
            }
        }
        
        // no exact match, try based on class hierarchy
        for (Class<?> e: ClassUtils.AllExtendedClasses.apply(type)) {
            for (ExcelSerializer<?> ExcelSerializer: find(e, serializers)) {
                return Some((ExcelSerializer<T>)ExcelSerializer);
            }
        }
        
        // no match, try based on inheritance
        for (Map.Entry<? extends Class<?>, ExcelSerializer<?>> e: serializers.entrySet()) {
            if (e.getKey().isAssignableFrom(type)) {
                return Some((ExcelSerializer<T>)e.getValue());
            }
        }
        
        // for primitives, try corresponding object serializer
        if (type.isPrimitive()) {
            for (ExcelSerializer<?> csvSerializer: find(ClassUtils.toObjectClass(type), serializers)) {
                return Some((ExcelSerializer<T>)csvSerializer);
            }
        }
        
        // try an array
        if (type.isArray()) {
            return Some((ExcelSerializer<T>)serializers.get(Array.class));
        }
        
        // try a class explicitly marked to be serialized as a bean
        if (type.isAnnotationPresent(JsonSerializeAsBean.class)) {
            return Some((ExcelSerializer<T>)serializers.get(JsonSerializeAsBean.class));
        }
        
        return None();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> columns(Class<?> type) {
        Option<ExcelSerializer<Object>> serializer = resolveSerializer(type);
        return serializer.isDefined() ? serializer.get().columns(this, (Class<Object>) type) : emptyList();
    }
}
