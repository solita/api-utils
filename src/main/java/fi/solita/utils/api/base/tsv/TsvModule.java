package fi.solita.utils.api.base.tsv;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.repeat;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.tsv.TsvSerializer.Cells;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Tuple;

public class TsvModule {

    public final Map<Class<?>, TsvSerializer<?>> serializers;

    public TsvModule(Map<Class<?>, TsvSerializer<?>> serializers) {
        this.serializers = serializers;
    }
    
    @SuppressWarnings("unchecked")
    public <T> TsvSerializer.Cells serialize(T obj) {
        Assert.notNull(obj);
        return serialize(obj, obj instanceof Option && ((Option<?>)obj).isDefined() ? ((Option<?>)obj).get().getClass() : (Class<T>)obj.getClass());
    }
    
    public <T> TsvSerializer.Cells serialize(T obj, Class<?> type) {
        if (obj == null || !Option.class.isAssignableFrom(type) && obj instanceof Option && !((Option<?>)obj).isDefined()) {
            List<String> cols = columns(type);
            return new Cells(repeat("", cols.size()), "").withHeaders(cols);
        }
        
        if (!Option.class.isAssignableFrom(type) && obj instanceof Option && ((Option<?>)obj).isDefined()) {
            return serialize(((Option<?>)obj).get(), type);
        }
        
        for (TsvSerializer<Object> ser: resolveSerializer(obj.getClass())) {
            Cells ret = ser.render(this, obj);
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
        throw new RuntimeException("No Tsv serializer for type: " + obj.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private <T> Option<TsvSerializer<T>> resolveSerializer(Class<?> type) {
        TsvSerializer<?> ret = serializers.get(type);
        if (ret != null) {
            return Some((TsvSerializer<T>)ret);
        }
        
        // try direct interface implementations
        for (Class<?> e: type.getInterfaces()) {
            for (TsvSerializer<?> TsvSerializer: find(e, serializers)) {
                return Some((TsvSerializer<T>)TsvSerializer);
            }
        }
        
        // no exact match, try based on class hierarchy
        for (Class<?> e: ClassUtils.AllExtendedClasses.apply(type)) {
            for (TsvSerializer<?> TsvSerializer: find(e, serializers)) {
                return Some((TsvSerializer<T>)TsvSerializer);
            }
        }
        
        // no match, try based on inheritance
        for (Map.Entry<? extends Class<?>, TsvSerializer<?>> e: serializers.entrySet()) {
            if (e.getKey().isAssignableFrom(type)) {
                return Some((TsvSerializer<T>)e.getValue());
            }
        }
        
        // for primitives, try corresponding object serializer
        if (type.isPrimitive()) {
            for (TsvSerializer<?> TsvSerializer: find(ClassUtils.toObjectClass(type), serializers)) {
                return Some((TsvSerializer<T>)TsvSerializer);
            }
        }
        
        // try an array
        if (type.isArray()) {
            return Some((TsvSerializer<T>)serializers.get(Array.class));
        }
        
        // try a class explicitly marked to be serialized as a bean
        if (type.isAnnotationPresent(JsonSerializeAsBean.class)) {
            return Some((TsvSerializer<T>)serializers.get(JsonSerializeAsBean.class));
        }
        
        return None();
    }

    @SuppressWarnings("unchecked")
    public List<String> columns(Class<?> type) {
        Option<TsvSerializer<Object>> serializer = resolveSerializer(type);
        return serializer.isDefined() ? serializer.get().columns(this, (Class<Object>) type) : emptyList();
    }
}
