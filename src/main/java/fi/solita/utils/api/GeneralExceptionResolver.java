package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;

import fi.solita.utils.api.filtering.FilterParser;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.api.util.ExceptionUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.RedundantPropertiesException;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.UnavailableContentTypeException;
import fi.solita.utils.api.util.RequestUtil.ETags;
import fi.solita.utils.api.util.RequestUtil.LoopsInPropertyNameException;


public abstract class GeneralExceptionResolver<REQ,RESP> implements Ordered {
    
    protected abstract void respondError(RESP response, int status, String message);
    
    protected abstract ETags getETags(REQ request);

    @SuppressWarnings("unused")
    public ModelAndView resolveException(REQ request, RESP response, Object handler, Exception ex) {
        for (NotFoundException e: ExceptionUtils.findCauseFromHierarchy(ex, NotFoundException.class)) {
            for (List<String> etags: getETags(request).ifMatch) {
                if (etags.contains("*")) {
                    // "14.24 If-Match: if "*" is given and no current entity exists ... MUST return a 412"
                    respondError(response, 412, "Precondition failed");
                    return new ModelAndView();
                }
            }
            respondError(response, 404, "Not found");
            return new ModelAndView();
        }


        for (RequestUtil.EventStreamNotAccepted e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.EventStreamNotAccepted.class)) {
            respondError(response, 406, "Requested content-type is not available for the requested resource for now");
            return new ModelAndView();
        }
        for (UnavailableContentTypeException e: ExceptionUtils.findCauseFromHierarchy(ex, UnavailableContentTypeException.class)) {
            respondError(response, 406, "Requested content-type is not available for the requested resource for now");
            return new ModelAndView();
        }
        for (RequestUtil.QueryParametersMustBeInAlphabeticalOrderException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParametersMustBeInAlphabeticalOrderException.class)) {
            respondError(response, 400, "Query string parameters must be in alphabetical order");
            return new ModelAndView();
        }
        for (RequestUtil.QueryParametersMustNotBeDuplicatedException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParametersMustNotBeDuplicatedException.class)) {
            respondError(response, 400, "Query string parameters must not be duplicated");
            return new ModelAndView();
        }
        for (RequestUtil.IllegalQueryParametersException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.IllegalQueryParametersException.class)) {
            respondError(response, 400, "Unknown query string parameters: " + mkString(", ", e.getUnknownParameters()));
            return new ModelAndView();
        }
        for (RequestUtil.QueryParameterMustBeInAlphabeticalOrderException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)) {
            respondError(response, 400, "Values of query parameter '" + e.paramName + "' must be in alphabetical order");
            return new ModelAndView();
        }
        for (RequestUtil.QueryParameterMustNotContainDuplicatesException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterMustNotContainDuplicatesException.class)) {
            respondError(response, 400, "Values of query parameter '" + e.paramName + "' must not contain duplicates");
            return new ModelAndView();
        }
        for (RequestUtil.QueryParameterValuesMustBeInLowercaseException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterValuesMustBeInLowercaseException.class)) {
            respondError(response, 400, "Values of query parameters must be in lowercase");
            return new ModelAndView();
        }
        for (RequestUtil.FilteringNotSupportedWithPaginationException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.FilteringNotSupportedWithPaginationException.class)) {
            respondError(response, 400, "Row filtering (cql_filter) is not supported together with pagination (count/startIndex)");
            return new ModelAndView();
        }
        for (RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException.class)) {
            respondError(response, 400, "Spatial filtering cannot be used together with bbox parameter");
            return new ModelAndView();
        }
        for (Filtering.SpatialFilteringRequiresGeometryPropertyException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.SpatialFilteringRequiresGeometryPropertyException.class)) {
            respondError(response, 400, "Spatial filtering can only be done for a geometry column. Not '" + e.filteringProperty.getValue() + "' but one of: " + mkString(",", e.geometryProperties));
            return new ModelAndView();
        }
        for (Filtering.CannotFilterByResolvableException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.CannotFilterByResolvableException.class)) {
            respondError(response, 400, "Cannot filter by a resolved property: " + e.filterProperty);
            return new ModelAndView();
        }
        for (MemberUtil.UnknownPropertyNameException e: ExceptionUtils.findCauseFromHierarchy(ex, MemberUtil.UnknownPropertyNameException.class)) {
            respondError(response, 400, "Unknown propertyName: " + e.propertyName.getValue());
            return new ModelAndView();
        }
        for (RedundantPropertiesException e: ExceptionUtils.findCauseFromHierarchy(ex, RedundantPropertiesException.class)) {
            respondError(response, 400, "Redundant values in propertyName: " + mkString(",", map(PropertyName_.getValue, e.propertyNames)));
            return new ModelAndView();
        }
        for (LoopsInPropertyNameException e: ExceptionUtils.findCauseFromHierarchy(ex, LoopsInPropertyNameException.class)) {
            respondError(response, 400, "Loops in propertyName");
            return new ModelAndView();
        }
        for (Includes.InvalidResolvableExclusionException e: ExceptionUtils.findCauseFromHierarchy(ex, Includes.InvalidResolvableExclusionException.class)) {
            respondError(response, 400, "Invalid exclusion of resolvable member: " + e.member.getName() + ". Put the negation sign in front of the external propertyname, e.g. instead of '-foo.bar' use 'foo.-bar'");
            return new ModelAndView();
        }
        for (ResolvableMemberProvider.CannotResolveAsFormatException e: ExceptionUtils.findCauseFromHierarchy(ex, ResolvableMemberProvider.CannotResolveAsFormatException.class)) {
            respondError(response, 400, "Cannot use resolvable properties with " + e.format.name());
            return new ModelAndView();
        }
        for (Filtering.FilterPropertyNotFoundException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.FilterPropertyNotFoundException.class)) {
            respondError(response, 400, "Property used for filtering (" + e.filterProperty.getValue() + ") not found in result. Use 'propertyName' parameter to define suitable set of properties");
            return new ModelAndView();
        }
        for (Filtering.CannotFilterByStructureException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.CannotFilterByStructureException.class)) {
            respondError(response, 400, "Cannot filter by structure");
            return new ModelAndView();
        }
        
        for (FilterParser.IllegalPointException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.IllegalPointException.class)) {
            respondError(response, 400, "Illegal coordinate in filtering: " + e.point);
            return new ModelAndView();
        }
        for (FilterParser.IllegalPolygonException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.IllegalPolygonException.class)) {
            respondError(response, 400, "Illegal polygon in filtering: " + e.polygon);
            return new ModelAndView();
        }
        for (FilterParser.FirstCoordinateMustEqualLastCoordinateException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.FirstCoordinateMustEqualLastCoordinateException.class)) {
            respondError(response, 400, "First coordinate of a polygon must match the last coordinate in filtering: " + e.first + " / " + e.last);
            return new ModelAndView();
        }
        
        for (FunctionProvider.UnknownFunctionException e: ExceptionUtils.findCauseFromHierarchy(ex, FunctionProvider.UnknownFunctionException.class)) {
            respondError(response, 400, "Unknown function: " + e.functionName);
            return new ModelAndView();
        }
        for (FunctionProvider.UnsupportedFunctionForPropertyException e: ExceptionUtils.findCauseFromHierarchy(ex, FunctionProvider.UnsupportedFunctionForPropertyException.class)) {
            respondError(response, 400, "Unsupported function '" + e.functionName + "' for property: " + e.propertyName);
            return new ModelAndView();
        }
        
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
