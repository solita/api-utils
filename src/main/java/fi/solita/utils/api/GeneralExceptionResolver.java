package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
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
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.api.util.RequestUtil.LoopsInPropertyNameException;


public class GeneralExceptionResolver implements HandlerExceptionResolver, Ordered {

    @SuppressWarnings("unused")
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            for (NotFoundException e: ExceptionUtils.findCauseFromHierarchy(ex, NotFoundException.class)) {
                for (List<String> etags: RequestUtil.getETags(request).ifMatch) {
                    if (etags.contains("*")) {
                        // "14.24 If-Match: if "*" is given and no current entity exists ... MUST return a 412"
                        ResponseUtil.respondError(response, HttpStatus.PRECONDITION_FAILED);
                        return new ModelAndView();
                    }
                }
                ResponseUtil.respondError(response, HttpStatus.NOT_FOUND);
                return new ModelAndView();
            }


            for (RequestUtil.EventStreamNotAccepted e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.EventStreamNotAccepted.class)) {
                ResponseUtil.respondError(response, HttpStatus.NOT_ACCEPTABLE.value(), "Requested content-type is not available for the requested resource for now");
                return new ModelAndView();
            }
            for (RequestUtil.UnavailableContentTypeException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.UnavailableContentTypeException.class)) {
                ResponseUtil.respondError(response, HttpStatus.NOT_ACCEPTABLE.value(), "Requested content-type is not available for the requested resource for now");
                return new ModelAndView();
            }
            for (RequestUtil.QueryParametersMustBeInAlphabeticalOrderException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParametersMustBeInAlphabeticalOrderException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Query string parameters must be in alphabetical order");
                return new ModelAndView();
            }
            for (RequestUtil.QueryParametersMustNotBeDuplicatedException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParametersMustNotBeDuplicatedException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Query string parameters must not be duplicated");
                return new ModelAndView();
            }
            for (RequestUtil.IllegalQueryParametersException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.IllegalQueryParametersException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unknown query string parameters: " + mkString(", ", e.getUnknownParameters()));
                return new ModelAndView();
            }
            for (RequestUtil.QueryParameterMustBeInAlphabeticalOrderException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Values of query parameter '" + e.paramName + "' must be in alphabetical order");
                return new ModelAndView();
            }
            for (RequestUtil.QueryParameterMustNotContainDuplicatesException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterMustNotContainDuplicatesException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Values of query parameter '" + e.paramName + "' must not contain duplicates");
                return new ModelAndView();
            }
            for (RequestUtil.QueryParameterValuesMustBeInLowercaseException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.QueryParameterValuesMustBeInLowercaseException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Values of query parameters must be in lowercase");
                return new ModelAndView();
            }
            for (RequestUtil.FilteringNotSupportedWithPaginationException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.FilteringNotSupportedWithPaginationException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Row filtering (cql_filter) is not supported together with pagination (count/startIndex)");
                return new ModelAndView();
            }
            for (RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Spatial filtering cannot be used together with bbox parameter");
                return new ModelAndView();
            }
            for (Filtering.SpatialFilteringRequiresGeometryPropertyException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.SpatialFilteringRequiresGeometryPropertyException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Spatial filtering can only be done for a geometry column. Not '" + e.filteringProperty.getValue() + "' but one of: " + mkString(",", e.geometryProperties));
                return new ModelAndView();
            }
            for (Filtering.CannotFilterByResolvableException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.CannotFilterByResolvableException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Cannot filter by a resolved property: " + e.filterProperty);
                return new ModelAndView();
            }
            for (MemberUtil.UnknownPropertyNameException e: ExceptionUtils.findCauseFromHierarchy(ex, MemberUtil.UnknownPropertyNameException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unknown propertyName: " + e.propertyName.getValue());
                return new ModelAndView();
            }
            for (RedundantPropertiesException e: ExceptionUtils.findCauseFromHierarchy(ex, RedundantPropertiesException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Redundant values in propertyName: " + mkString(",", map(PropertyName_.getValue, e.propertyNames)));
                return new ModelAndView();
            }
            for (LoopsInPropertyNameException e: ExceptionUtils.findCauseFromHierarchy(ex, LoopsInPropertyNameException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Loops in propertyName");
                return new ModelAndView();
            }
            for (Includes.InvalidResolvableExclusionException e: ExceptionUtils.findCauseFromHierarchy(ex, Includes.InvalidResolvableExclusionException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Invalid exclusion of resolvable member: " + e.member.getName() + ". Put the negation sign in front of the external propertyname, e.g. instead of '-foo.bar' use 'foo.-bar'");
                return new ModelAndView();
            }
            for (ResolvableMemberProvider.CannotResolveAsFormatException e: ExceptionUtils.findCauseFromHierarchy(ex, ResolvableMemberProvider.CannotResolveAsFormatException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Cannot use resolvable properties with " + e.format.name());
                return new ModelAndView();
            }
            for (Filtering.FilterPropertyNotFoundException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.FilterPropertyNotFoundException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Property used for filtering (" + e.filterProperty.getValue() + ") not found in result. Use 'propertyName' parameter to define suitable set of properties");
                return new ModelAndView();
            }
            for (Filtering.CannotFilterByStructureException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.CannotFilterByStructureException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Cannot filter by structure");
                return new ModelAndView();
            }
            
            for (FilterParser.IllegalPointException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.IllegalPointException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal coordinate in filtering: " + e.point);
                return new ModelAndView();
            }
            for (FilterParser.IllegalPolygonException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.IllegalPolygonException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal polygon in filtering: " + e.polygon);
                return new ModelAndView();
            }
            for (FilterParser.FirstCoordinateMustEqualLastCoordinateException e: ExceptionUtils.findCauseFromHierarchy(ex, FilterParser.FirstCoordinateMustEqualLastCoordinateException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "First coordinate of a polygon must match the last coordinate in filtering: " + e.first + " / " + e.last);
                return new ModelAndView();
            }
            
            for (FunctionProvider.UnknownFunctionException e: ExceptionUtils.findCauseFromHierarchy(ex, FunctionProvider.UnknownFunctionException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unknown function: " + e.functionName);
                return new ModelAndView();
            }
            for (FunctionProvider.UnsupportedFunctionForPropertyException e: ExceptionUtils.findCauseFromHierarchy(ex, FunctionProvider.UnsupportedFunctionForPropertyException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unsupported function '" + e.functionName + "' for property: " + e.propertyName);
                return new ModelAndView();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // just in case the next resolver doesn't set this
        ResponseUtil.setAccessControlHeaders(response);
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
