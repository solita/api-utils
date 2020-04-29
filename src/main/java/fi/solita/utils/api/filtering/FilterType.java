package fi.solita.utils.api.filtering;

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
}
