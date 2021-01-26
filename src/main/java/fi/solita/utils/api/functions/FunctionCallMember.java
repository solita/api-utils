package fi.solita.utils.api.functions;

import java.lang.reflect.AccessibleObject;

import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.meta.MetaNamedMember;

public class FunctionCallMember<T> implements MetaNamedMember<T,Object> {
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
    
    public FunctionCallMember<T> applied(Apply<?,?> f) {
        return new AppliedFunctionCallMember<T>(propertyName, original, f);
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

class AppliedFunctionCallMember<T> extends FunctionCallMember<T> {
    private final Apply<Object,Object> f;

    @SuppressWarnings("unchecked")
    public <V> AppliedFunctionCallMember(PropertyName propertyName, MetaNamedMember<? super T,?> original, Apply<?,?> f) {
        super(propertyName, original);
        this.f = (Apply<Object, Object>) f;
    }
    
    @Override
    public Object apply(T t) {
        return f.apply(original.apply(t));
        
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
        AppliedFunctionCallMember<?> other = (AppliedFunctionCallMember<?>) obj;
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