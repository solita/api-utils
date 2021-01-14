package fi.solita.utils.api.types;

import java.util.regex.Pattern;

import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Predicate;

public abstract class PropertyName implements Comparable<PropertyName> {

    protected final String value;

    PropertyName(String value) {
        this.value = value;
    }
    
    public static final PropertyName of(String value) {
        if (FunctionProvider.isFunctionCall(value)) {
            return new FunctionCallPropertyName(value);
        } else {
            return new RegularPropertyName(value);
        }
    }
    
    private static class RegularPropertyName extends PropertyName {
        RegularPropertyName(String value) {
            super(value);
        }
        
        public boolean isEmpty(FunctionProvider fp) {
            return value.isEmpty();
        }
        
        public boolean isPrefixOf(FunctionProvider fp, final String longer) {
            return (longer + ".").startsWith(value + ".");
        }
        
        public boolean isFunctionCall() {
            return false;
        }
        
        public PropertyName omitExclusion() {
            if (isExclusion()) {
                return new RegularPropertyName(value.substring(1));
            }
            throw new IllegalStateException("Was not an exclusion: " + this);
        }
        
        public PropertyName stripPrefix(FunctionProvider fp, final String prefix) {
            return new RegularPropertyName(value.equals(prefix) ? "" : value.replaceFirst(Pattern.quote(prefix + "."), ""));
        }
        
        public boolean startsWith(FunctionProvider fp, final String prefix) {
            return (value + ".").startsWith(prefix + ".");
        }
        
        public PropertyName toProperty(FunctionProvider fp) {
            return this;
        }
        
        public Object applyFunction(FunctionProvider fp, Object obj) {
            return obj;
        }
    }
    
    private static class FunctionCallPropertyName extends PropertyName {
        FunctionCallPropertyName(String value) {
            super(value);
        }

        public boolean isEmpty(FunctionProvider fp) {
            return fp.argumentMatches(value, new Predicate<String>() {
                @Override
                public boolean accept(String candidate) {
                    return candidate.isEmpty();
                }
            });
        }
        
        public boolean isPrefixOf(FunctionProvider fp, final String longer) {
            return fp.argumentMatches(value, new Predicate<String>() {
                @Override
                public boolean accept(String candidate) {
                    return (longer + ".").startsWith(candidate + ".");
                }
            });
        }
        
        public boolean isFunctionCall() {
            return true;
        }
        
        public PropertyName omitExclusion() {
            if (isExclusion()) {
                return PropertyName.of(value.substring(1));
            }
            throw new IllegalStateException("Was not an exclusion: " + this);
        }
        
        public PropertyName stripPrefix(FunctionProvider fp, final String prefix) {
            return new FunctionCallPropertyName(fp.mapArgument(value, new Function1<String,String>() {
                @Override
                public String apply(String t) {
                    return t.equals(prefix) ? "" : t.replaceFirst(Pattern.quote(prefix + "."), "");
                }
            }));
        }
        
        public boolean startsWith(FunctionProvider fp, final String prefix) {
            return fp.argumentMatches(value, new Predicate<String>() {
                @Override
                public boolean accept(String candidate) {
                    return (candidate + ".").startsWith(prefix + ".");
                }
            });
        }
        
        public PropertyName toProperty(FunctionProvider fp) {
            return new RegularPropertyName(fp.toArgument(value));
        }
        
        public Object applyFunction(FunctionProvider fp, Object obj) {
            return fp.apply(value, obj);
        }
    }
    
    public String getValue() {
        return value;
    }
    
    public boolean isExclusion() {
        return value.startsWith("-");
    }
    
    public abstract boolean isEmpty(FunctionProvider fp);
    
    public abstract boolean isPrefixOf(FunctionProvider fp, final String longer);
    
    public abstract boolean isFunctionCall();
    
    public abstract PropertyName omitExclusion();
    
    public abstract PropertyName stripPrefix(FunctionProvider fp, final String prefix);
    
    public abstract boolean startsWith(FunctionProvider fp, final String prefix);
    
    public abstract PropertyName toProperty(FunctionProvider fp);
    
    public abstract Object applyFunction(FunctionProvider fp, Object obj);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!PropertyName.class.isInstance(obj))
            return false;
        PropertyName other = (PropertyName) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PropertyName [value=" + value + "]";
    }

    @Override
    public int compareTo(PropertyName o) {
        return value.compareTo(o.value);
    }
}
