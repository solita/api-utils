package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import fi.solita.utils.api.types.PropertyName;

public final class Filter {
    public final FilterType pattern;
    public final PropertyName property;
    public final List<Literal> literals;
    
    public Filter(FilterType pattern, PropertyName property, Literal... literals) {
        this.pattern = pattern;
        this.property = property;
        this.literals = newList(literals);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
        result = prime * result + ((property == null) ? 0 : property.hashCode());
        result = prime * result + ((literals == null) ? 0 : literals.hashCode());
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
        Filter other = (Filter) obj;
        if (pattern == null) {
            if (other.pattern != null)
                return false;
        } else if (!pattern.equals(other.pattern))
            return false;
        if (property == null) {
            if (other.property != null)
                return false;
        } else if (!property.equals(other.property))
            return false;
        if (literals == null) {
            if (other.literals != null)
                return false;
        } else if (!literals.equals(other.literals))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Filter [pattern=" + pattern + ", property=" + property + ", values=" + literals + "]";
    }
}