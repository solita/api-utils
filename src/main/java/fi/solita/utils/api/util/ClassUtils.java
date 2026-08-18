package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.it;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.headOptional;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.FunctionalA.concat;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.databind.type.SimpleType;

import fi.solita.utils.api.DynamicMember;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.lens.Builder;

public class ClassUtils {
    
    public static <T> List<T> toList(Iterable<T> xs) {
        return xs == null ? null : xs instanceof List ? (List<T>)xs : newList(xs);
    }

    /**
     * This is because enums with class bodies seem to differ from other enums...
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<Class<T>> getEnumType(Class<T> type) {
        if (type.isEnum()) {
            return Optional.of((Class<T>) type);
        }
        if (type.getEnclosingClass() != null && type.getEnclosingClass().isEnum()) {
            return Optional.of((Class<T>) type.getEnclosingClass());
        }
        return Optional.empty();
    }
    
    public static Optional<Type> getFirstTypeArgument(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType)type).getActualTypeArguments();
            return headOptional(newList(args));
        } else if (type instanceof SimpleType && ((SimpleType)type).containedTypeCount() > 0) {
            return Optional.of(((SimpleType)type).containedType(0));
        }
        return Optional.empty();
    }
    
    public static Optional<Type> getSecondTypeArgument(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType)type).getActualTypeArguments();
            return headOptional(tail(newList(args)));
        } else if (type instanceof SimpleType && ((SimpleType)type).containedTypeCount() > 1) {
            return Optional.of(((SimpleType)type).containedType(1));
        }
        return Optional.empty();
    }
    
    public static Type getGenericType(AccessibleObject member) {
        return member instanceof Field
            ? ((Field)member).getGenericType()
            : member instanceof Method
            ? ((Method)member).getGenericReturnType()
            : member instanceof DynamicMember.DynamicAccessibleObject
            ? ((DynamicMember.DynamicAccessibleObject)member).type
            : null;
    }

    public static Class<?> typeClass(Type type) {
        for (Class<?> ret: it(resolveClass(type))) {
            return ret;
        }
        throw new IllegalArgumentException("Could not handle Type: " + type.getClass());
    }
    
    public static Optional<Class<?>> resolveClass(Type type) {
        if (type instanceof ParameterizedType) {
            return resolveClass(((ParameterizedType)type).getRawType());
        } else if (type instanceof Class) {
            return Optional.of((Class<?>) type);
        } else if (type instanceof TypeVariable<?>) {
            return resolveClass(((TypeVariable<?>) type).getBounds()[0]);
        } else if (type instanceof ResolvedType) {
            return Optional.of(((ResolvedType)type).getRawClass());
        } else if (type instanceof Builder.MapType) {
            return Optional.of(Map.class);
        } else {
            return Optional.empty();
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
    
    public static final Function<Class<?>, Iterable<Class<?>>> AllExtendedClasses = new Function<Class<?>, Iterable<Class<?>>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Iterable<Class<?>> apply(Class<?> source) {
            return source.getSuperclass() == null ? Collections.<Class<?>>emptyList() : (Iterable<Class<?>>)(Object)(cons(source.getSuperclass(), flatMap(this, it(Optional.ofNullable(source.getSuperclass())))));
        }
    };
    
    public static final Function<Class<?>, Iterable<Field>> AllDeclaredApplicationFields = new Function<Class<?>, Iterable<Field>>() {
        @Override
        public Iterable<Field> apply(Class<?> source) {
            if ( !source.getPackage().getName().startsWith("java") ) {
                return concat(source.getDeclaredFields(), flatMap(this, it(Optional.ofNullable(source.getSuperclass()))));
            }
            return newMutableList();
        }
    };
    
    public static final Predicate<Member> PublicMembers = t -> Modifier.isPublic(t.getModifiers());
    
    public static final Predicate<Member> StaticMembers = t -> Modifier.isStatic(t.getModifiers());
}

