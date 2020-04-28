package fi.solita.utils.api.types;

import java.util.regex.Pattern;

import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Predicate;

public class PropertyName implements Comparable<PropertyName> {

    public final String value;

    public PropertyName(String value) {
        this.value = value;
    }
    
    public boolean isExclusion() {
        return value.startsWith("-");
    }
    
    public PropertyName omitExclusion() {
        if (isExclusion()) {
            return new PropertyName(value.substring(1));
        }
        throw new IllegalStateException("Was not an exclusion: " + this);
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
    
    public PropertyName stripPrefix(FunctionProvider fp, final String prefix) {
        return new PropertyName(fp.mapArgument(value, new Function1<String,String>() {
            @Override
            public String apply(String t) {
                return t.replaceFirst(Pattern.quote(prefix), "");
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
    
    public PropertyName removeFirstPart(FunctionProvider fp) {
        return new PropertyName(fp.mapArgument(value, new Function1<String,String>() {
            @Override
            public String apply(String t) {
                int i = t.indexOf('.');
                return i == -1 ? "" : t.substring(i+1);
            }
        }));
    }

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
        if (getClass() != obj.getClass())
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
