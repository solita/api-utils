package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.mkString;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.util.ExceptionUtils;
import fi.solita.utils.api.util.ResponseUtil;


public class HttpSerializationExceptionResolver implements HandlerExceptionResolver, Ordered {
    
    @SuppressWarnings("unused")
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            for (HttpSerializers.InvalidValueException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidValueException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal " + e.type + " '" + e.value + "'" + (e.validValues.isEmpty() ? "" : ". Accepted values: " + mkString(", ", e.validValues)));
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidFilterException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidFilterException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal filter expression. Supported filters are: " + mkString(", ", e.validValues));
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidStartIndexException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidStartIndexException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal 'startIndex'. Must be a positive integer");
                return new ModelAndView();
            }
            
            for (HttpSerializers.BeginAndEndMustMatchException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.BeginAndEndMustMatchException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Unexpected interval. Only instants are accepted for the time being");
                return new ModelAndView();
            }
            for (HttpSerializers.IntervalNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.IntervalNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal interval. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
            
            for (HttpSerializers.LocalDateNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.LocalDateNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal localdate. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidTimeZoneException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidTimeZoneException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal time zone");
                return new ModelAndView();
            }
            
            for (HttpSerializers.DateTimeNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.DateTimeNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Illegal datetime. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
