package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.util.List;

import fi.solita.utils.api.filtering.Filter;
import fi.solita.utils.api.filtering.FilterType;
import fi.solita.utils.api.filtering.Filter_;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Predicates;

public final class Filters {
    public static final List<String> SUPPORTED_OPERATIONS = newList("=", "<>", "<", ">", "<=", ">=", "_ [NOT] BETWEEN _ AND _", "_ [NOT] LIKE '%'", "_ [NOT] ILIKE '%'", "_ [NOT] IN (_,_)", "_ IS [NOT] NULL", "INTERSECTS(_,_)");
    
    public static final Filters EMPTY = new Filters(Collections.<List<Filter>>emptyList());
    
    public final List<List<Filter>> or;
    
    public Filters(Filter a, Filter... and) {
        this(Collections.<List<Filter>>newList(newList(cons(a, and))));
    }
    
    public Filters(List<List<Filter>> or) {
        for (List<Filter> and: or) {
            Assert.False(and.isEmpty());
        }
        this.or = newList(filter(not(Predicates.<Filter,List<Filter>>empty()), or));
    }
    
    public List<List<Filter>> spatialFilters() {
        return newList(map(new Apply<List<Filter>,List<Filter>>() {
            @Override
            public List<Filter> apply(List<Filter> and) {
                return newList(filter(Filter_.pattern.andThen(equalTo(FilterType.INTERSECTS)), and));
            }
        }, or));
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((or == null) ? 0 : or.hashCode());
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
        if (or == null) {
            if (other.or != null)
                return false;
        } else if (!or.equals(other.or))
            return false;
        return true;
    }
}
