package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalA.head;
import static fi.solita.utils.functional.FunctionalA.last;
import static fi.solita.utils.functional.FunctionalC.init;
import static fi.solita.utils.functional.FunctionalC.tail;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.solita.utils.api.types.PropertyName;

public class FilterParser {

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
    private static final String attribute = "([a-z][a-zA-Z0-9_.]*)";
    private static final String polygon = "(POLYGON\\s*\\(.+?\\))";
    private static final String literal = "(-?\\d+(?:\\.\\d+)?|true|false|'(?:[^']|'')*'|\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ|P\\d+Y(?:\\d+M(?:\\d+D(?:T\\d+H(?:\\d+M(?:\\d+S))))))";
    private static final String like_pattern = "('(?:[^']|'')*')";
    
    private static final Pattern EQUAL       = Pattern.compile(FilterParser.attribute + "="  + literal + " AND ");
    private static final Pattern NOT_EQUAL   = Pattern.compile(FilterParser.attribute + "<>" + literal + " AND ");
    private static final Pattern LT          = Pattern.compile(FilterParser.attribute + "<"  + literal + " AND ");
    private static final Pattern GT          = Pattern.compile(FilterParser.attribute + ">"  + literal + " AND ");
    private static final Pattern LTE         = Pattern.compile(FilterParser.attribute + "<=" + literal + " AND ");
    private static final Pattern GTE         = Pattern.compile(FilterParser.attribute + ">=" + literal + " AND ");
    
    private static final Pattern BETWEEN     = Pattern.compile(FilterParser.attribute + " BETWEEN " + literal + " AND " + literal + " AND ");
    private static final Pattern NOT_BETWEEN = Pattern.compile(FilterParser.attribute + " NOT BETWEEN " + literal + " AND " + literal + " AND ");
    
    private static final Pattern LIKE        = Pattern.compile(FilterParser.attribute + " LIKE " + like_pattern + " AND ");
    private static final Pattern NOT_LIKE    = Pattern.compile(FilterParser.attribute + " NOT LIKE " + like_pattern + " AND ");
    private static final Pattern ILIKE       = Pattern.compile(FilterParser.attribute + " ILIKE " + like_pattern + " AND ");
    private static final Pattern NOT_ILIKE   = Pattern.compile(FilterParser.attribute + " NOT ILIKE " + like_pattern + " AND ");
    
    private static final Pattern IN          = Pattern.compile(FilterParser.attribute + " IN \\((" + literal + "(?:," + literal + ")*)\\) AND ");
    private static final Pattern NOT_IN      = Pattern.compile(FilterParser.attribute + " NOT IN \\((" + literal + "(?:," + literal + ")*)\\) AND ");
    
    private static final Pattern NULL        = Pattern.compile(FilterParser.attribute + " IS NULL AND ");
    private static final Pattern NOT_NULL    = Pattern.compile(FilterParser.attribute + " IS NOT NULL AND ");
    
    private static final Pattern INTERSECTS  = Pattern.compile("INTERSECTS\\(" + FilterParser.attribute + "," + FilterParser.polygon + "\\) AND ");

    private static final Pattern inlist = Pattern.compile("(" + literal + "),");
    
    static String stripLiteral(String expr) {
        return (expr.startsWith("'") && expr.endsWith("'") ? init(tail(expr)) : expr).replace("''", "'");
    }
    
    public static final List<Filter> parse(String cql_filter) {
        cql_filter += " AND ";
        List<Filter> filters = newList();
        
        for (Pattern p: newList(EQUAL, NOT_EQUAL, LT, GT, LTE, GTE)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p == EQUAL     ? FilterType.EQUAL :
                                       p == NOT_EQUAL ? FilterType.NOT_EQUAL :
                                       p == LT        ? FilterType.LT :
                                       p == GT        ? FilterType.GT :
                                       p == LTE       ? FilterType.LTE :
                                       p == GTE       ? FilterType.GTE : null,
                                       PropertyName.of(matcher.group(1)), stripLiteral(matcher.group(2))));
            }
        }
        
        for (Pattern p: newList(BETWEEN, NOT_BETWEEN)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p == BETWEEN     ? FilterType.BETWEEN :
                                       p == NOT_BETWEEN ? FilterType.NOT_BETWEEN : null,
                                       PropertyName.of(matcher.group(1)), stripLiteral(matcher.group(2)), stripLiteral(matcher.group(3))));
            }
        }
        
        for (Pattern p: newList(LIKE, NOT_LIKE, ILIKE, NOT_ILIKE)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p == LIKE      ? FilterType.LIKE :
                                       p == NOT_LIKE  ? FilterType.NOT_LIKE :
                                       p == ILIKE     ? FilterType.ILIKE :
                                       p == NOT_ILIKE ? FilterType.NOT_ILIKE : null,
                                       PropertyName.of(matcher.group(1)), stripLiteral(matcher.group(2))));
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
                filters.add(new Filter(p == IN     ? FilterType.IN :
                                       p == NOT_IN ? FilterType.NOT_IN : null,
                                       PropertyName.of(matcher.group(1)), newArray(String.class, map(FilterParser_.stripLiteral, inargs))));
            }
        }
        
        for (Pattern p: newList(NULL, NOT_NULL)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                filters.add(new Filter(p == NULL     ? FilterType.NULL :
                                       p == NOT_NULL ? FilterType.NOT_NULL : null,
                                       PropertyName.of(matcher.group(1))));
            }
        }
        
        for (Pattern p: newList(INTERSECTS)) {
            Matcher matcher = p.matcher(cql_filter);
            while (matcher.find()) {
                String wkt = matcher.group(2);
                checkWKT(wkt);
                filters.add(new Filter(FilterType.INTERSECTS, PropertyName.of(matcher.group(1)), wkt));
            }
        }
        
        return filters;
    }
    
    private static final Pattern POLYGON = Pattern.compile("^POLYGON\\s*\\(\\s*\\((.+)\\)\\s*\\)$");
    private static final Pattern POINT = Pattern.compile("\\d+(\\.\\d+)?\\s+\\d+(\\.\\d+)?");
    private static void checkWKT(String wkt) throws FilterParser.FirstCoordinateMustEqualLastCoordinateException, FilterParser.IllegalPointException, FilterParser.IllegalPolygonException {
        Matcher matcher = POLYGON.matcher(wkt);
        if (!matcher.matches() || matcher.groupCount() != 1) {
            throw new FilterParser.IllegalPolygonException(wkt);
        }
        String[] points = matcher.group(1).split("\\s*,\\s*");
        for (String p: points) {
            if (!POINT.matcher(p).matches()) {
                throw new FilterParser.IllegalPointException(p);
            }
        }
        if (!head(points).equals(last(points))) {
            throw new FilterParser.FirstCoordinateMustEqualLastCoordinateException(head(points), last(points));
        }
    }
}