package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concatMap;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.meta.MetaNamedMember;

class WrapperType<T> implements ParameterizedType {
    private final Type type;
    private final Class<?> wrapper;
    
    public WrapperType(Class<?> wrapper, Type type) {
        this.type = type;
        this.wrapper = wrapper;
    }
    
    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] {type};
    }

    @Override
    public Type getRawType() {
        return wrapper;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}

public abstract class NestedMember<S,T> implements MetaNamedMember<S,T> {
    public final MetaNamedMember<S,?> parent;
    public final MetaNamedMember<?,?> child;
    private final String name;
    
    public static final <S,U,T> NestedMember<S,T> of(MetaNamedMember<S, U> p, MetaNamedMember<? super U,T> c) {
        return new NestedMember<S,T>(p, c) {
            @Override
            public T apply(S s) {
                return c.apply(p.apply(s));
            }
            @Override
            public Type resultingType() {
                return ClassUtils.getGenericType(c.getMember());
            }
            @Override
            public String toString() {
                return "of:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Optional<T>> ofOpt_(MetaNamedMember<S, Optional<U>> p, MetaNamedMember<? super U,T> c) {
        return new NestedMember<S,Optional<T>>(p, c) {
            @Override
            public Optional<T> apply(S s) {
                return ((Optional<U>)p.apply(s)).map(u -> c.apply(u));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Optional.class, ClassUtils.getGenericType(c.getMember()));
            }
            @Override
            public String toString() {
                return "ofOpt_:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofIt_(MetaNamedMember<S, ? extends Iterable<U>> p, MetaNamedMember<? super U,T> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(map(c, p.apply(s)));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, ClassUtils.getGenericType(c.getMember()));
            }
            @Override
            public String toString() {
                return "ofIt_:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofIt_Opt(MetaNamedMember<S, ? extends Iterable<U>> p, MetaNamedMember<? super U,Optional<T>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(concatMap(c, p.apply(s)));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get());
            }
            @Override
            public String toString() {
                return "ofIt_Opt:" + super.toString();
            }
        };
    }

    public static final <S,U,T> NestedMember<S,Iterable<T>> ofIt_It(MetaNamedMember<S, ? extends Iterable<U>> p, MetaNamedMember<? super U,? extends Iterable<T>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(flatMap(c, p.apply(s)));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get());
            }
            @Override
            public String toString() {
                return "ofIt_It:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofIt_OptIt(MetaNamedMember<S, ? extends Iterable<U>> p, MetaNamedMember<? super U,Optional<Iterable<T>>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(flatten(concatMap(c, p.apply(s))));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get()).get());
            }
            @Override
            public String toString() {
                return "ofIt_OptIt:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Optional<T>> ofOpt_Opt(MetaNamedMember<S, ? extends Optional<U>> p, MetaNamedMember<? super U,Optional<T>> c) {
        return new NestedMember<S,Optional<T>>(p, c) {
            @Override
            public Optional<T> apply(S s) {
                return p.apply(s).flatMap(u -> c.apply(u));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Optional.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getGenericType(c.getMember()));
            }
            @Override
            public String toString() {
                return "ofOpt_Opt:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOpt_It(MetaNamedMember<S, ? extends Optional<U>> p, MetaNamedMember<? super U,Iterable<T>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(p.apply(s).map(c).orElse(emptyList()));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get());
            }
            @Override
            public String toString() {
                return "ofOpt_It:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOpt_OptIt(MetaNamedMember<S, ? extends Optional<U>> p, MetaNamedMember<? super U,Optional<Iterable<T>>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(p.apply(s).flatMap(c).orElse(emptyList()));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get()).get());
            }
            @Override
            public String toString() {
                return "ofOpt_OptIt:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOptIt_(MetaNamedMember<S, ? extends Optional<? extends Iterable<U>>> p, MetaNamedMember<? super U,? extends T> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                @SuppressWarnings("unchecked")
                Iterable<U> pp = ((Optional<Iterable<U>>)p.apply(s)).orElse(emptyList());
                return newList(map(c, pp));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, ClassUtils.getGenericType(c.getMember()));
            }
            @Override
            public String toString() {
                return "ofOptIt_:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOptIt_Opt(MetaNamedMember<S, ? extends Optional<? extends Iterable<U>>> p, MetaNamedMember<? super U,? extends Optional<T>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                return newList(p.apply(s).map(u -> concatMap(c, u)).orElse(emptyList()));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get());
            }
            @Override
            public String toString() {
                return "ofOptIt_Opt:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOptIt_It(MetaNamedMember<S, ? extends Optional<? extends Iterable<U>>> p, MetaNamedMember<? super U,? extends Iterable<T>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                @SuppressWarnings("unchecked")
                Optional<Iterable<U>> pp = (Optional<Iterable<U>>) p.apply(s);
                return newList(flatMap(c, pp.orElse(emptyList())));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get());
            }
            @Override
            public String toString() {
                return "ofOptIt_It:" + super.toString();
            }
        };
    }
    
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOptIt_OptIt(MetaNamedMember<S, ? extends Optional<? extends Iterable<U>>> p, MetaNamedMember<? super U,? extends Optional<Iterable<T>>> c) {
        return new NestedMember<S,Iterable<T>>(p, c) {
            @Override
            public Iterable<T> apply(S s) {
                @SuppressWarnings("unchecked")
                Optional<Iterable<U>> pp = (Optional<Iterable<U>>) p.apply(s);
                return newList(flatten(concatMap(c, pp.orElse(emptyList()))));
            }
            @Override
            public Type resultingType() {
                return new WrapperType<>(Iterable.class, c instanceof NestedMember ? ((NestedMember<?,?>) c).resultingType() : ClassUtils.getFirstTypeArgument(ClassUtils.getFirstTypeArgument(ClassUtils.getGenericType(c.getMember())).get()).get());
            }
            @Override
            public String toString() {
                return "ofOptIt_OptIt:" + super.toString();
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    public static final <S> NestedMember<S,?> ofUnchecked(MetaNamedMember<?, ?> p, MetaNamedMember<?, ?> c, boolean flattenChildIterables) {
        Type parentType = p instanceof NestedMember ? ((NestedMember<?,?>)p).resultingType() : ClassUtils.getGenericType(p.getMember());
        Class<?> parentClass = ClassUtils.resolveClass(parentType).get();
        Optional<Type> parentSubtype = ClassUtils.getFirstTypeArgument(parentType);
        Optional<Class<?>> parentSubclass = parentSubtype.flatMap(ClassUtils::resolveClass);
        
        Type childType = c instanceof NestedMember ? ((NestedMember<?,?>)c).resultingType() : ClassUtils.getGenericType(c.getMember());
        Class<?> childClass = ClassUtils.resolveClass(childType).get();
        Optional<Type> childSubtype = ClassUtils.getFirstTypeArgument(childType);
        Optional<Class<?>> childSubclass = childSubtype.flatMap(ClassUtils::resolveClass);
        
        boolean parentOptionalIterable = parentClass.equals(Optional.class) && parentSubclass.map(Iterable.class::isAssignableFrom).orElse(false);
        boolean parentOptional = parentClass.equals(Optional.class);
        boolean parentIterable = parentClass.equals(Iterable.class);
        
        boolean childOptionalIterable = c instanceof NestedMember && childClass.equals(Optional.class) && childSubclass.map(Iterable.class::isAssignableFrom).orElse(false);
        boolean childOptional = childClass.equals(Optional.class);
        boolean childIterable = childClass.equals(Iterable.class);
        
        if (flattenChildIterables && parentOptionalIterable && childOptionalIterable) {
            return ofOptIt_OptIt((MetaNamedMember<S, Optional<Iterable<Object>>>)p, (MetaNamedMember<Object, Optional<Iterable<Object>>>)c);
        } else if (flattenChildIterables && parentOptionalIterable && childIterable) {
            return ofOptIt_It((MetaNamedMember<S, Optional<Iterable<Object>>>)p, (MetaNamedMember<Object, Iterable<Object>>)c);
        } else if (parentOptionalIterable && childOptional) {
            return ofOptIt_Opt((MetaNamedMember<S, Optional<Iterable<Object>>>)p, (MetaNamedMember<Object, Optional<Object>>)c);
        } else if (parentOptionalIterable) {
            return ofOptIt_((MetaNamedMember<S, Optional<Iterable<Object>>>)p, (MetaNamedMember<Object, Object>)c);
        } else if (flattenChildIterables && parentOptional && childOptionalIterable) {
            return ofOpt_OptIt((MetaNamedMember<S, Optional<Object>>)p, (MetaNamedMember<Object, Optional<Iterable<Object>>>)c);
        } else if (flattenChildIterables && parentOptional && childIterable) {
            return ofOpt_It((MetaNamedMember<S, Optional<Object>>)p, (MetaNamedMember<Object, Iterable<Object>>)c);
        } else if (parentOptional && childOptional) {
            return ofOpt_Opt((MetaNamedMember<S, Optional<Object>>)p, (MetaNamedMember<Object, Optional<Object>>)c);
        } else if (flattenChildIterables && parentIterable && childOptionalIterable) {
            return ofIt_OptIt((MetaNamedMember<S, Iterable<Object>>)p, (MetaNamedMember<Object, Optional<Iterable<Object>>>)c);
        } else if (flattenChildIterables && parentIterable && childIterable) {
            return ofIt_It((MetaNamedMember<S, Iterable<Object>>)p, (MetaNamedMember<Object, Iterable<Object>>)c);
        } else if (parentIterable && childOptional) {
            return ofIt_Opt((MetaNamedMember<S, Iterable<Object>>)p, (MetaNamedMember<Object, Optional<Object>>)c);
        } else if (parentIterable) {
            return ofIt_((MetaNamedMember<S, Iterable<Object>>)p, (MetaNamedMember<Object, Object>)c);
        } else if (parentOptional) {
            return ofOpt_((MetaNamedMember<S, Optional<Object>>)p, (MetaNamedMember<Object, Object>)c);
        } else {
            return of((MetaNamedMember<S, Object>)p, (MetaNamedMember<Object, Object>)c);
        }
    }

    private NestedMember(MetaNamedMember<S, ?> parent, MetaNamedMember<?,?> child) {
        this.parent = parent;
        this.child = child;
        this.name = parent.getName() + "." + child.getName();
    }
    
    public abstract Type resultingType();

    @Override
    public AccessibleObject getMember() {
        return child.getMember();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((child == null) ? 0 : child.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NestedMember<?,?> other = (NestedMember<?,?>) obj;
        if (child == null) {
            if (other.child != null)
                return false;
        } else if (!child.equals(other.child))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
