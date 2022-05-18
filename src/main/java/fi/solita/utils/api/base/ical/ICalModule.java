package fi.solita.utils.api.base.ical;

import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.Array;
import java.util.Map;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Option;
import net.fortuna.ical4j.model.component.CalendarComponent;

public class ICalModule {

    public final Map<Class<?>, ICalSerializer<?>> serializers;

    public ICalModule(Map<Class<?>, ICalSerializer<?>> serializers) {
        this.serializers = serializers;
    }
    
    @SuppressWarnings("unchecked")
    public <T> CalendarComponent serialize(T obj) {
        Assert.notNull(obj);
        return serialize(obj, obj instanceof Option && ((Option<?>)obj).isDefined() ? ((Option<?>)obj).get().getClass() : (Class<T>)obj.getClass());
    }
    
    public <T> CalendarComponent serialize(T obj, Class<?> type) {
        if (obj == null || !Option.class.isAssignableFrom(type) && obj instanceof Option && !((Option<?>)obj).isDefined()) {
            return null;
        }
        
        for (ICalSerializer<Object> ser: resolveSerializer(obj.getClass())) {
            return ser.serialize(this, obj);
        }
        throw new RuntimeException("No iCal serializer for type: " + obj.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private <T> Option<ICalSerializer<T>> resolveSerializer(Class<?> type) {
        ICalSerializer<?> ret = serializers.get(type);
        if (ret != null) {
            return Some((ICalSerializer<T>)ret);
        }
        
        // try direct interface implementations
        for (Class<?> e: type.getInterfaces()) {
            for (ICalSerializer<?> icalSerializer: find(e, serializers)) {
                return Some((ICalSerializer<T>)icalSerializer);
            }
        }
        
        // no exact match, try based on class hierarchy
        for (Class<?> e: ClassUtils.AllExtendedClasses.apply(type)) {
            for (ICalSerializer<?> icalSerializer: find(e, serializers)) {
                return Some((ICalSerializer<T>)icalSerializer);
            }
        }
        
        // no match, try based on inheritance
        for (Map.Entry<? extends Class<?>, ICalSerializer<?>> e: serializers.entrySet()) {
            if (e.getKey().isAssignableFrom(type)) {
                return Some((ICalSerializer<T>)e.getValue());
            }
        }
        
        // for primitives, try corresponding object serializer
        if (type.isPrimitive()) {
            for (ICalSerializer<?> icalSerializer: find(ClassUtils.toObjectClass(type), serializers)) {
                return Some((ICalSerializer<T>)icalSerializer);
            }
        }
        
        // try an array
        if (type.isArray()) {
        	for (ICalSerializer<?> icalSerializer: find(Array.class, serializers)) {
                return Some((ICalSerializer<T>)icalSerializer);
            }
        }
        
        // try a class explicitly marked to be serialized as a bean
        if (type.isAnnotationPresent(JsonSerializeAsBean.class)) {
        	for (ICalSerializer<?> icalSerializer: find(JsonSerializeAsBean.class, serializers)) {
        		return Some((ICalSerializer<T>)icalSerializer);
        	}
        }
        
        return None();
    }
}
