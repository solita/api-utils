package fi.solita.utils.api.base;

import static fi.solita.utils.functional.FunctionalM.find;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

import org.rendersnake.Renderable;

import fi.solita.utils.api.ClassUtils;
import fi.solita.utils.api.JsonSerializeAsBean;

public class HtmlModule {

    public final Map<Class<?>, HtmlSerializer<?>> renderables;

    public HtmlModule(Map<Class<?>, HtmlSerializer<?>> serializers) {
        this.renderables = serializers;
    }
    
    @SuppressWarnings("unchecked")
    public <T> Renderable toRenderable(T obj) {
        if (obj == null) {
            return ((HtmlSerializer<T>)renderables.get(Void.class)).toRenderable(this, obj);
        }
        
        HtmlSerializer<?> ret = renderables.get(obj.getClass());
        if (ret != null) {
            return ((HtmlSerializer<T>)ret).toRenderable(this, obj);
        }
        
        // no exact match, try based on class hierarchy
        for (Class<?> e: ClassUtils.AllExtendedClasses.apply(obj.getClass())) {
            for (HtmlSerializer<?> htmlSerializer: find(e, renderables)) {
                return ((HtmlSerializer<T>)htmlSerializer).toRenderable(this, obj);
            }
        }
        
        // no match, try based on inheritance
        for (Entry<? extends Class<?>, HtmlSerializer<?>> e: renderables.entrySet()) {
            if (e.getKey().isInstance(obj)) {
                return ((HtmlSerializer<T>)e.getValue()).toRenderable(this, obj);
            }
        }
        
        // try an array
        if (obj.getClass().isArray()) {
            return ((HtmlSerializer<T>)renderables.get(Array.class)).toRenderable(this, obj);
        }
        
        // try a Dto
        if (obj.getClass().isAnnotationPresent(JsonSerializeAsBean.class)) {
            return ((HtmlSerializer<T>)renderables.get(JsonSerializeAsBean.class)).toRenderable(this, obj);
        }
        
        throw new RuntimeException("No HTML serializer for type: " + obj.getClass());
    }
}
