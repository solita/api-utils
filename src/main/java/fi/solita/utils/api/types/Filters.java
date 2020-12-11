package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.util.List;

import fi.solita.utils.api.filtering.Filter;
import fi.solita.utils.api.filtering.FilterType;
import fi.solita.utils.api.filtering.Filter_;

public final class Filters {
    public static final List<String> SUPPORTED_OPERATIONS = newList("=", "<>", "<", ">", "<=", ">=", "_ [NOT] BETWEEN _ AND _", "_ [NOT] LIKE '%'", "_ [NOT] ILIKE '%'", "_ [NOT] IN (_,_)", "_ IS [NOT] NULL", "INTERSECTS(_,_)");
    
    public static final Filters EMPTY = new Filters();
    
    public final List<Filter> filters;
    
    public Filters(Filter... filters) {
        this(newList(filters));
    }
    
    public Filters(List<Filter> filters) {
        this.filters = newList(filters);
    }
    
    public List<Filter> spatialFilters() {
        return newList(filter(Filter_.pattern.andThen(equalTo(FilterType.INTERSECTS)), filters));
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filters == null) ? 0 : filters.hashCode());
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
        Filters other = (Filters) obj;
        if (filters == null) {
            if (other.filters != null)
                return false;
        } else if (!filters.equals(other.filters))
            return false;
        return true;
    }
}
