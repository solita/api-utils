package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.FunctionalA.concat;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Transformer;

public class ClassUtils {

    /**
     * This is because enums with class bodies seem to differ from other enums...
     */
    @SuppressWarnings("unchecked")
    public static <T> Option<Class<T>> getEnumType(Class<T> type) {
        if (type.isEnum()) {
            return Some((Class<T>) type);
        }
        if (type.getEnclosingClass() != null && type.getEnclosingClass().isEnum()) {
            return Some((Class<T>) type.getEnclosingClass());
        }
        return None();
    }

    public static Class<?> typeClass(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType)type).getActualTypeArguments();
            return args.length == 0 ? typeClass(((ParameterizedType)type).getRawType()) : typeClass(args[0]);
        } else if (type instanceof Class) {
            return (Class<?>) type;
        } else {
            throw new IllegalArgumentException("Could not handle Type: " + type.getClass());
        }
    }
    
    public static Class<?> toObjectClass(Class<?> primitiveClass) {
        if (primitiveClass.isPrimitive()) {
            if (boolean.class.equals(primitiveClass)) {
                return Boolean.class;
            } else if (byte.class.equals(primitiveClass)) {
                return Byte.class;
            } else if (char.class.equals(primitiveClass)) {
                return Character.class;
            } else if (short.class.equals(primitiveClass)) {
                return Short.class;
            } else if (int.class.equals(primitiveClass)) {
                return Integer.class;
            } else if (long.class.equals(primitiveClass)) {
                return Long.class;
            } else if (float.class.equals(primitiveClass)) {
                return Float.class;
            } else if (double.class.equals(primitiveClass)) {
                return Double.class;
            }
        }
        return primitiveClass;
    }
    
    public static final Transformer<Class<?>, Iterable<Class<?>>> AllExtendedClasses = new Transformer<Class<?>, Iterable<Class<?>>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Iterable<Class<?>> transform(Class<?> source) {
            return source.getSuperclass() == null ? Collections.<Class<?>>emptyList() : (Iterable<Class<?>>)(Object)(cons(source.getSuperclass(), flatMap(this, Option.of(source.getSuperclass()))));
        }
    };
    
    public static final Transformer<Class<?>, Iterable<Field>> AllDeclaredApplicationFields = new Transformer<Class<?>, Iterable<Field>>() {
        @Override
        public Iterable<Field> transform(Class<?> source) {
            if ( !source.getPackage().getName().startsWith("java") ) {
                return concat(source.getDeclaredFields(), flatMap(this, Option.of(source.getSuperclass())));
            }
            return newList();
        }
    };
    
    public static final Function1<Member, Boolean> PublicMembers = new Function1<Member, Boolean>() {
        @Override
        public Boolean apply(Member t) {
            return Modifier.isPublic(t.getModifiers());
        }
    };
    
    public static final Function1<Member, Boolean> StaticMembers = new Function1<Member, Boolean>() {
        @Override
        public Boolean apply(Member t) {
            return Modifier.isStatic(t.getModifiers());
        }
    };
}

