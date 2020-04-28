package fi.solita.utils.api;

import java.lang.reflect.AccessibleObject;

import fi.solita.utils.meta.MetaNamedMember;

public final class FunctionCallMember<T> implements MetaNamedMember<T,Object> {
    public final MetaNamedMember<? super T,?> original;
    public final PropertyName propertyName;

    public FunctionCallMember(PropertyName propertyName, MetaNamedMember<? super T,?> original) {
        this.original = original;
        this.propertyName = propertyName;
    }
    
    @Override
    public Object apply(T t) {
        return original.apply(t);
    }

    @Override
    public AccessibleObject getMember() {
        return original.getMember();
    }
    
    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
        result = prime * result + ((original == null) ? 0 : original.hashCode());
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
        FunctionCallMember<?> other = (FunctionCallMember<?>) obj;
        if (propertyName == null) {
            if (other.propertyName != null)
                return false;
        } else if (!propertyName.equals(other.propertyName))
            return false;
        if (original == null) {
            if (other.original != null)
                return false;
        } else if (!original.equals(other.original))
            return false;
        return true;
    }

    
}