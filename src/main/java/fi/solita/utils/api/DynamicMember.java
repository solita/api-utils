package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableLinkedMap;
import static fi.solita.utils.functional.Functional.headOption;
import static fi.solita.utils.functional.Functional.map;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class DynamicMember<V> implements MetaNamedMember<Map<String,V>,V> {
    
    public static final class DynamicAccessibleObject extends AccessibleObject {
        public final String name;
        public final Type type;
        
        public DynamicAccessibleObject(String name, Type type) {
            this.name = name;
            this.type = type;
        }
        
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
            return (T[]) Array.newInstance(annotationClass, 0);
        }
        
        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }
    }
    
    private final String name;
    private final Type type;
    
    public static Collection<DynamicMember<Object>> of(Map<String,?> m) {
        return newList(map(DynamicMember_.$().ap(m), m.keySet()));
    }
    
    @SuppressWarnings("unchecked")
    public static <V> Builder<Map<String,V>>[] builders(Map<String,V> m) {
        return newArray(Builder.class, (Collection<Builder<Map<String,V>>>)(Object)allBuilders(m).values());
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<Set<String>, Builder<Map<String,V>>> allBuilders(Object o) {
        if (!(o instanceof Map || o instanceof Iterable)) {
            return emptyMap();
        }
        Map<Set<String>, Builder<Map<String,V>>> ret = newMutableLinkedMap();
        if (o instanceof Map) {
            if (!ret.containsKey(((Map<String,V>)o).keySet())) {
                ret.put(((Map<String,V>)o).keySet(), builder((Map<String,V>)o));
            }
            for (Map.Entry<String, V> e: ((Map<String,V>)o).entrySet()) {
                ret.putAll(allBuilders(e.getValue()));
            }
        } else if (o instanceof Iterable) {
            for (Object v: (Iterable<?>)o) {
                ret.putAll(allBuilders(v));
            }
        }
        return ret;
    }
    
    private static <V> Builder<Map<String,V>> builder(final Map<String,V> m) {
        List<? extends Apply<Map<String,V>, ? extends Object>> members = newList(map(new Apply<String, MetaNamedMember<Map<String,V>, V>>() {
            @Override
            public MetaNamedMember<Map<String,V>, V> apply(final String k) {
                return new MetaNamedMember<Map<String,V>, V>() {
                    @Override
                    public V apply(Map<String, V> t) {
                        return t.get(k);
                    }

                    @Override
                    public AccessibleObject getMember() {
                        return new DynamicAccessibleObject(k, m.get(k) != null ? m.get(k).getClass() : Object.class);
                    }

                    @Override
                    public String getName() {
                        return k;
                    }};
            }
        }, m.keySet()));
        return Builder.ofMap(m.keySet(), members);
    }
    
    DynamicMember(Map<String,?> m, String key) {
        this.name = key;
        this.type = m.get(key) != null ? resolveType(m.get(key)) : Object.class;
    }

    @SuppressWarnings("unchecked")
    private static Type resolveType(Object object) {
        return object instanceof Map
            ? new Builder.MapType<Object>(((Map<Object,Object>)object).keySet())
            : object instanceof Iterable
            ? resolveType(headOption((Iterable<Object>)object).getOrElse(object.getClass()))
            : object.getClass();
    }

    @Override
    public V apply(Map<String,V> t) {
        return t.get(name);
    }

    @Override
    public AccessibleObject getMember() {
        return new DynamicAccessibleObject(name, type);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        DynamicMember other = (DynamicMember) obj;
        return Objects.equals(name, other.name) && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
