package fi.solita.utils.api.base.csv;

import static fi.solita.utils.functional.Collections.it;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.repeat;
import static fi.solita.utils.functional.FunctionalM.find;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.csv.CsvSerializer.Cells;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Tuple;

public class CsvModule {

    public final Map<Class<?>, CsvSerializer<?>> serializers;

    public CsvModule(Map<Class<?>, CsvSerializer<?>> serializers) {
        this.serializers = serializers;
    }
    
    @SuppressWarnings("unchecked")
    public <T> CsvSerializer.Cells serialize(T obj) {
        Assert.notNull(obj);
        return serialize(obj, obj instanceof Optional && ((Optional<?>)obj).isPresent() ? ((Optional<?>)obj).get().getClass() : (Class<T>)obj.getClass());
    }
    
    public <T> CsvSerializer.Cells serialize(T obj, Class<?> type) {
        if (obj == null || !Optional.class.isAssignableFrom(type) && obj instanceof Optional && !((Optional<?>)obj).isPresent()) {
            List<String> cols = columns(type);
            return new Cells(repeat("", cols.size()), "").withHeaders(cols);
        }
        
        if (!Optional.class.isAssignableFrom(type) && obj instanceof Optional && ((Optional<?>)obj).isPresent()) {
            return serialize(((Optional<?>)obj).get(), type);
        }
        
        for (CsvSerializer<Object> ser: it(resolveSerializer(obj.getClass()))) {
            Cells ret = ser.render(this, obj);
            if (obj instanceof Optional) {
                if (Optional.class.isAssignableFrom(type)) {
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
        throw new RuntimeException("No CSV serializer for type: " + obj.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private <T> Optional<CsvSerializer<T>> resolveSerializer(Class<?> type) {
        CsvSerializer<?> ret = serializers.get(type);
        if (ret != null) {
            return Optional.of((CsvSerializer<T>)ret);
        }
        
        // try direct interface implementations
        for (Class<?> e: type.getInterfaces()) {
            for (CsvSerializer<?> csvSerializer: it(find(e, serializers))) {
                return Optional.of((CsvSerializer<T>)csvSerializer);
            }
        }
        
        // no exact match, try based on class hierarchy
        for (Class<?> e: ClassUtils.AllExtendedClasses.apply(type)) {
            for (CsvSerializer<?> csvSerializer: it(find(e, serializers))) {
                return Optional.of((CsvSerializer<T>)csvSerializer);
            }
        }
        
        // no match, try based on inheritance
        for (Map.Entry<? extends Class<?>, CsvSerializer<?>> e: serializers.entrySet()) {
            if (e.getKey().isAssignableFrom(type)) {
                return Optional.of((CsvSerializer<T>)e.getValue());
            }
        }
        
        // for primitives, try corresponding object serializer
        if (type.isPrimitive()) {
            for (CsvSerializer<?> csvSerializer: it(find(ClassUtils.toObjectClass(type), serializers))) {
                return Optional.of((CsvSerializer<T>)csvSerializer);
            }
        }
        
        // try an array
        if (type.isArray()) {
            return Optional.of((CsvSerializer<T>)serializers.get(Array.class));
        }
        
        // try a class explicitly marked to be serialized as a bean
        if (type.isAnnotationPresent(JsonSerializeAsBean.class)) {
            return Optional.of((CsvSerializer<T>)serializers.get(JsonSerializeAsBean.class));
        }
        
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public List<String> columns(Class<?> type) {
        Optional<CsvSerializer<Object>> serializer = resolveSerializer(type);
        return serializer.isPresent() ? serializer.get().columns(this, (Class<Object>) type) : Collections.<String>emptyList();
    }
}
