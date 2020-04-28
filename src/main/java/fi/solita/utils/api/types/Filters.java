package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalA.head;
import static fi.solita.utils.functional.FunctionalA.last;
import static fi.solita.utils.functional.FunctionalC.init;
import static fi.solita.utils.functional.FunctionalC.tail;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.solita.utils.api.PropertyName;
import fi.solita.utils.api.types.Filters_.Filter_;
import fi.solita.utils.functional.Option;

public final class Filters {
    public static final class IllegalPolygonException extends RuntimeException {
        public final String polygon;
        public IllegalPolygonException(String polygon) {
            this.polygon = polygon;
        }
    }
    
    public static final class IllegalPointException extends RuntimeException {
        public final String point;
        public IllegalPointException(String point) {
            this.point = point;
        }
    }
    
    public static final class FirstCoordinateMustEqualLastCoordinateException extends RuntimeException {
        public final String first;
        public final String last;
        public FirstCoordinateMustEqualLastCoordinateException(String first, String last) {
            this.first = first;
            this.last = last;
        }
    }
    
    public static final List<String> SUPPORTED_OPERATIONS = newList("=", "<>", "<", ">", "<=", ">=", "_ [NOT] BETWEEN _ AND _", "_ [NOT] LIKE '%'", "_ [NOT] ILIKE '%'", "_ [NOT] IN (_,_)", "_ IS [NOT] NULL", "INTERSECTS(_,_)");
    
    private static final String attribute = "([a-z][a-zA-Z0-9_.]*)";
    private static final String polygon = "(POLYGON\\s*\\(.+?\\))";
    private static final String literal = "(-?\\d+(?:\\.\\d+)?|true|false|'(?:[^']|'')*'|\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ|P\\d+Y(?:\\d+M(?:\\d+D(?:T\\d+H(?:\\d+M(?:\\d+S))))))";
    private static final String like_pattern = "('(?:[^']|'')*')";
    
    public static final Pattern EQUAL       = Pattern.compile(attribute + "="  + literal + " AND ");
    public static final Pattern NOT_EQUAL   = Pattern.compile(attribute + "<>" + literal + " AND ");
    public static final Pattern LT          = Pattern.compile(attribute + "<"  + literal + " AND ");
    public static final Pattern GT          = Pattern.compile(attribute + ">"  + literal + " AND ");
    public static final Pattern LTE         = Pattern.compile(attribute + "<=" + literal + " AND ");
    public static final Pattern GTE         = Pattern.compile(attribute + ">=" + literal + " AND ");
    
    public static final Pattern BETWEEN     = Pattern.compile(attribute + " BETWEEN " + literal + " AND " + literal + " AND ");
    public static final Pattern NOT_BETWEEN = Pattern.compile(attribute + " NOT BETWEEN " + literal + " AND " + literal + " AND ");
    
    public static final Pattern LIKE        = Pattern.compile(attribute + " LIKE " + like_pattern + " AND ");
    public static final Pattern NOT_LIKE    = Pattern.compile(attribute + " NOT LIKE " + like_pattern + " AND ");
    public static final Pattern ILIKE       = Pattern.compile(attribute + " ILIKE " + like_pattern + " AND ");
    public static final Pattern NOT_ILIKE   = Pattern.compile(attribute + " NOT ILIKE " + like_pattern + " AND ");
    
    public static final Pattern IN          = Pattern.compile(attribute + " IN \\((" + literal + "(?:," + literal + ")*)\\) AND ");
    public static final Pattern NOT_IN      = Pattern.compile(attribute + " NOT IN \\((" + literal + "(?:," + literal + ")*)\\) AND ");
    
    public static final Pattern NULL        = Pattern.compile(attribute + " IS NULL AND ");
    public static final Pattern NOT_NULL    = Pattern.compile(attribute + " IS NOT NULL AND ");
    
    public static final Pattern INTERSECTS  = Pattern.compile("INTERSECTS\\(" + attribute + "," + polygon + "\\) AND ");

    private static final Pattern inlist = Pattern.compile("(" + literal + "),");
    
    public static final Filters EMPTY = new Filters();
    
    static String stripLiteral(String expr) {
        return (expr.startsWith("'") && expr.endsWith("'") ? init(tail(expr)) : expr).replace("''", "'");
    }
    
    public static final Option<Filters> parse(String cql_filter) {
        cql_filter += " AND ";
        List<Filter> filters = newList();
        
        for (Pattern p: newList(EQUAL, NOT_EQUAL, LT, GT, LTE, GTE)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p, new PropertyName(matcher.group(1)), stripLiteral(matcher.group(2))));
            }
        }
        
        for (Pattern p: newList(BETWEEN, NOT_BETWEEN)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p, new PropertyName(matcher.group(1)), stripLiteral(matcher.group(2)), stripLiteral(matcher.group(3))));
            }
        }
        
        for (Pattern p: newList(LIKE, NOT_LIKE, ILIKE, NOT_ILIKE)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p, new PropertyName(matcher.group(1)), stripLiteral(matcher.group(2))));
            }
        }
        
        for (Pattern p: newList(IN, NOT_IN)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                List<String> inargs = newList();
                Matcher m = inlist.matcher(matcher.group(2) + ",");
                while (m.find()) {
                    inargs.add(m.group(1));
                }
                filters.add(new Filter(p, new PropertyName(matcher.group(1)), newArray(String.class, map(Filters_.stripLiteral, inargs))));
            }
        }
        
        for (Pattern p: newList(NULL, NOT_NULL)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p, new PropertyName(matcher.group(1))));
            }
        }
        
        for (Pattern p: newList(INTERSECTS)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                String wkt = matcher.group(2);
                checkWKT(wkt);
                filters.add(new Filter(p, new PropertyName(matcher.group(1)), wkt));
            }
        }
        
        return filters.isEmpty() ? Option.<Filters>None() : Some(new Filters(filters));
    }
    
    static String[] split(String s, String regex) {
        return s.split(regex);
    }
    
    private static final Pattern POLYGON = Pattern.compile("^POLYGON\\s*\\(\\s*\\((.+)\\)\\s*\\)$");
    private static final Pattern POINT = Pattern.compile("\\d+(\\.\\d+)?\\s+\\d+(\\.\\d+)?");
    private static void checkWKT(String wkt) throws FirstCoordinateMustEqualLastCoordinateException, IllegalPointException, IllegalPolygonException {
        Matcher matcher = POLYGON.matcher(wkt);
        if (!matcher.matches() || matcher.groupCount() != 1) {
            throw new IllegalPolygonException(wkt);
        }
        String[] points = matcher.group(1).split("\\s*,\\s*");
        for (String p: points) {
            if (!POINT.matcher(p).matches()) {
                throw new IllegalPointException(p);
            }
        }
        if (!head(points).equals(last(points))) {
            throw new FirstCoordinateMustEqualLastCoordinateException(head(points), last(points));
        }
    }

    public final List<Filter> filters;
    
    Filters(Filter... filters) {
        this(newList(filters));
    }
    
    Filters(List<Filter> filters) {
        this.filters = newList(filters);
    }
    
    public List<Filter> spatialFilters() {
        return newList(filter(Filter_.pattern.andThen(equalTo(Filters.INTERSECTS)), filters));
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

    public static final class Filter {
        public final Pattern pattern;
        public final PropertyName property;
        public final List<String> values;
        
        Filter(Pattern pattern, PropertyName property, String... values) {
            this.pattern = pattern;
            this.property = property;
            this.values = newList(values);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
            result = prime * result + ((property == null) ? 0 : property.hashCode());
            result = prime * result + ((values == null) ? 0 : values.hashCode());
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
            if (values == null) {
                if (other.values != null)
                    return false;
            } else if (!values.equals(other.values))
                return false;
            return true;
        }
    }
}
