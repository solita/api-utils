package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.mkString;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;


public class GeneralExceptionResolver implements HandlerExceptionResolver, Ordered {

    @SuppressWarnings({ "unused", "unchecked" })
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
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Filtering is not supported together with pagination");
                return new ModelAndView();
            }
            for (RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException e: ExceptionUtils.findCauseFromHierarchy(ex, RequestUtil.SpatialFilteringCannotBeUsedWithBBOXException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Spatial filtering cannot be used together with bbox parameter");
                return new ModelAndView();
            }
            for (Filtering.SpatialFilteringRequiresGeometryPropertyException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.SpatialFilteringRequiresGeometryPropertyException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Spatial filtering can only be done for a geometry column. Not '" + e.filteringProperty + "' but one of: " + mkString(",", e.geometryProperties));
                return new ModelAndView();
            }
            for (MemberUtil.UnknownPropertyNameException e: ExceptionUtils.findCauseFromHierarchy(ex, MemberUtil.UnknownPropertyNameException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unknown propertyName: " + e.propertyName);
                return new ModelAndView();
            }
            for (Filtering.FilterPropertyNotFoundException e: ExceptionUtils.findCauseFromHierarchy(ex, Filtering.FilterPropertyNotFoundException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Property used for filtering (" + e.filterProperty + ") not found in result. Use 'propertyName' parameter to define suitable set of properties");
                return new ModelAndView();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // just in case the next resolver doesn't set this
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
