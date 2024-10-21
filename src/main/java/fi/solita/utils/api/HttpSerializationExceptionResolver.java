package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.mkString;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.util.ExceptionUtils;


public abstract class HttpSerializationExceptionResolver<REQ,RESP> implements Ordered {
    
    protected abstract void respondError(RESP response, int status, String message);
    
    /**
     * @param request  
     * @param handler 
     */
    @SuppressWarnings("unused")
    public ModelAndView resolveException(REQ request, RESP response, Object handler, Exception ex) {
        for (HttpSerializers.InvalidValueException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidValueException.class)) {
            respondError(response, 400, "Illegal " + e.type + " '" + e.value + "'" + (e.validValues.isEmpty() ? "" : ". Accepted values: " + mkString(", ", e.validValues)));
            return new ModelAndView();
        }
        for (HttpSerializers.InvalidFilterException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidFilterException.class)) {
            respondError(response, 400, "Illegal filter expression. Supported filters are: " + mkString(", ", e.validValues));
            return new ModelAndView();
        }
        for (HttpSerializers.InvalidStartIndexException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidStartIndexException.class)) {
            respondError(response, 400, "Illegal 'startIndex'. Must be a positive integer");
            return new ModelAndView();
        }
        
        for (HttpSerializers.BeginAndEndMustMatchException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.BeginAndEndMustMatchException.class)) {
            respondError(response, 400, "Unexpected interval. Only instants are accepted for the time being");
            return new ModelAndView();
        }
        for (HttpSerializers.IntervalNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.IntervalNotWithinLimitsException.class)) {
            respondError(response, 400, "Illegal interval. Accepted range: " + e.validStart + "-" + e.validEnd);
            return new ModelAndView();
        }
        
        for (HttpSerializers.LocalDateNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.LocalDateNotWithinLimitsException.class)) {
            respondError(response, 400, "Illegal localdate. Accepted range: " + e.validStart + "-" + e.validEnd);
            return new ModelAndView();
        }
        for (HttpSerializers.InvalidTimeZoneException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidTimeZoneException.class)) {
            respondError(response, 400, "Illegal time zone");
            return new ModelAndView();
        }
        
        for (HttpSerializers.DateTimeNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.DateTimeNotWithinLimitsException.class)) {
            respondError(response, 400, "Illegal datetime. Accepted range: " + e.validStart + "-" + e.validEnd);
            return new ModelAndView();
        }
        
        for (HttpSerializers.ExpectedSingletonException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.ExpectedSingletonException.class)) {
            respondError(response, 400, "Illegal list. Expected a single value");
            return new ModelAndView();
        }
        
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
