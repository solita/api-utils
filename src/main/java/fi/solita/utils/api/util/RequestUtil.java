package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.distinct;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.subtract;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.Functional.zip;
import static fi.solita.utils.functional.FunctionalA.last;
import static fi.solita.utils.functional.FunctionalC.drop;
import static fi.solita.utils.functional.FunctionalC.dropWhile;
import static fi.solita.utils.functional.FunctionalC.init;
import static fi.solita.utils.functional.FunctionalC.reverse;
import static fi.solita.utils.functional.FunctionalC.takeWhile;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.functional.ApplyBi;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class RequestUtil {

    public static class EventStreamNotAccepted extends RuntimeException {
    }

    public static class QueryParameterMustBeInAlphabeticalOrderException extends RuntimeException {
        public final String paramName;

        public QueryParameterMustBeInAlphabeticalOrderException(String paramName) {
            super(paramName);
            this.paramName = paramName;
        }
    }
    
    public static class QueryParameterMustNotContainDuplicatesException extends RuntimeException {
        public final String paramName;

        public QueryParameterMustNotContainDuplicatesException(String paramName) {
            this.paramName = paramName;
        }
    }
    
    public static class QueryParametersMustBeInAlphabeticalOrderException extends RuntimeException {
    }

    public static class QueryParametersMustNotBeDuplicatedException extends RuntimeException {
    }
    
    public static class QueryParameterValuesMustBeInLowercaseException extends RuntimeException {
    }
    
    public static class FilteringNotSupportedWithPaginationException extends RuntimeException {
    }
    
    public static class SpatialFilteringCannotBeUsedWithBBOXException extends RuntimeException {
    }
    
    public static class LoopsInPropertyNameException extends RuntimeException {
    }

    public static class IllegalQueryParametersException extends RuntimeException {
        private final List<String> unknownParameters;
        public IllegalQueryParametersException(List<String> unknownParameters) {
            super(mkString(", ", unknownParameters));
            this.unknownParameters = unknownParameters;
        }
        public List<String> getUnknownParameters() {
            return unknownParameters;
        }
    }
    
    public static class ETags {
        public final Option<List<String>> ifMatch;
        public final Option<List<String>> ifNoneMatch;

        public ETags(Option<List<String>> ifMatch, Option<List<String>> ifNoneMatch) {
            this.ifMatch = ifMatch;
            this.ifNoneMatch = ifNoneMatch;
        }
    }
    
    static String trim(String s) {
        return s.trim();
    }
    
    static final Option<List<String>> parseETags(Option<String> etags) {
        for (String s: etags) {
            return Some(newList(map(RequestUtil_.trim, s.split(","))));
        }
        return None();
    }
    
    static void assertAcceptHeader(List<String> accepts) {
        for (String accept: accepts) {
            if (accept.toLowerCase().startsWith("text/event-stream")) {
                throw new EventStreamNotAccepted();
            }
        }
    }

    static boolean inOrder(String a, String b) {
        return a.compareToIgnoreCase(b) <= 0;
    }
    
    static void assertQueryStringValid(Map<String, String[]> parameters, List<String> parameterNames, Set<String> caseIgnoredParams, String... acceptedParams) throws IllegalQueryParametersException, QueryParametersMustNotBeDuplicatedException, QueryParametersMustBeInAlphabeticalOrderException {
        List<String> unknownParameters = newList(subtract(parameterNames, newSet(acceptedParams)));
        if (!unknownParameters.isEmpty()) {
            throw new RequestUtil.IllegalQueryParametersException(unknownParameters);
        }
        
        if (parameterNames.size() != newSet(parameterNames).size()) {
            throw new RequestUtil.QueryParametersMustNotBeDuplicatedException();
        }
        
        if (size(filter(not(Function.<Boolean>id()), map((ApplyBi<String,String,Boolean>)RequestUtil_.inOrder, zip(parameterNames, tail(parameterNames))))) > 1) {
            throw new RequestUtil.QueryParametersMustBeInAlphabeticalOrderException();
        }
        
        if (parameterNames.contains("cql_filter") && (parameterNames.contains("count") || parameterNames.contains("startIndex"))) {
            throw new RequestUtil.FilteringNotSupportedWithPaginationException();
        }
        
        for (String[] cql_filters: find("cql_filter", parameters)) {
            if (parameterNames.contains("bbox")) {
                for (String cql_filter: cql_filters) {
                    if (cql_filter.contains("INTERSECTS") ||
                        cql_filter.contains("DISJOINT") ||
                        cql_filter.contains("CONTAINS") ||
                        cql_filter.contains("WITHIN") ||
                        cql_filter.contains("TOUCHES") ||
                        cql_filter.contains("CROSSES") ||
                        cql_filter.contains("OVERLAPS") ||
                        cql_filter.contains("RELATE")) {
                        throw new RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException();
                    }
                }
            }
        }
        
        for (String[] propertyNames: find("propertyName", parameters)) {
            for (String propertyName: propertyNames) {
                for (String pn: propertyName.split(",")) {
                    String[] parts = pn.split("[.]");
                    List<Pair<String, String>> pairs = newList(zip(parts, tail(parts)));
                    if (pairs.size() != size(distinct(pairs))) {
                        throw new RequestUtil.LoopsInPropertyNameException();
                    }
                }
            }
        }
        
        for (Map.Entry<String, String[]> param: parameters.entrySet()) {
            for (String v: param.getValue()) {
                if (!caseIgnoredParams.contains(param.getKey()) && !v.toLowerCase().equals(v)) {
                    throw new RequestUtil.QueryParameterValuesMustBeInLowercaseException();
                }
            }
        }
    }
    
    static List<String> split(String s, String regex) {
        return newList(s.split(regex));
    }

    public static final String getContextPath(String reqContextPath, Option<String> xForwardedPrefix) {
        for (String forwardedPrefix: xForwardedPrefix) {
            return (forwardedPrefix.endsWith("/") ? init(forwardedPrefix) : forwardedPrefix) + reqContextPath;
        }
        return reqContextPath;
    }
    
    public static final String getContextRelativePath(String servletPath, Option<String> pathInfo) {
        return servletPath + pathInfo.getOrElse("");
    }

    public static final String getAPIVersionRelativePath(String contextRelativePath) {
        return dropWhile(not(equalTo('/')), tail(contextRelativePath));
    }
    
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    
    public static final String getAPIVersionRelativePathWithoutRevision(String versionRelativePath) {
        String candidate = takeWhile(not(equalTo('/')), drop(1, versionRelativePath));
        return DIGITS.matcher(candidate).matches() ? dropWhile(not(equalTo('/')), drop(1, versionRelativePath)) : versionRelativePath;
    }
    
    public static final String getApiVersionBasePath(String contextPath, String contextRelativePath) {
        return contextPath + "/" + takeWhile(not(equalTo('/')), tail(contextRelativePath)) + "/";
    }

    public static final Option<String> resolveExtension(String path) {
        String[] paths = path.split("/");
        String lastPathPart = last(paths);
        if (!lastPathPart.contains(".")) {
            return None();
        }
        return Some(reverse(takeWhile(not(equalTo('.')), reverse(lastPathPart))));
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static final <T> List<MetaNamedMember<T,?>> toFields(List<? extends MetaNamedMember<? super T,?>> members, Iterable<PropertyName> propertyNames) {
        if (propertyNames == null) {
            return (List<MetaNamedMember<T, ?>>) members;
        }
        if (size(propertyNames) == 1 && head(propertyNames).isEmpty(FunctionProvider.NONE)) {
            return emptyList();
        }
        return (List<MetaNamedMember<T,?>>) (Object) newList(flatMap(MemberUtil_.<T>toMembers().ap(ResolvableMemberProvider.NONE, FunctionProvider.NONE, false, members), propertyNames));
    }

    public static final String API_KEY = "Api-Key";

    public static final String instant2string(DateTime instant) {
        return limit(instant).toString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC());
    }
    
    public static final String interval2stringRestrictedToInfinity(Interval interval) {
        return instant2string(interval.getStart()) + "/" + instant2string(interval.getEnd());
    }
    
    public static final DateTime limit(DateTime dt) {
        return dt.isBefore(HttpSerializers.VALID.getStart()) ? HttpSerializers.VALID.getStart() :
               dt.isAfter (HttpSerializers.VALID.getEnd())   ? HttpSerializers.VALID.getEnd() :
               dt;
    }

    public static final String intervalInfinity() {
        return interval2stringRestrictedToInfinity(HttpSerializers.VALID);
    }
}
