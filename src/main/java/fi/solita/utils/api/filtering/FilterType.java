package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newSet;

import java.util.Set;

public enum FilterType {
    EQUAL,
    NOT_EQUAL,
    LT,
    GT,
    LTE,
    GTE,
    
    BETWEEN,
    NOT_BETWEEN,
    
    LIKE,
    NOT_LIKE,
    ILIKE,
    NOT_ILIKE,
    
    IN,
    NOT_IN,
    
    NULL,
    NOT_NULL,
    
    INTERSECTS;
    
    public static final Set<FilterType> PATTERN_TYPES = newSet(LIKE, NOT_LIKE, ILIKE, NOT_ILIKE);
}
