package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.util.ExceptionUtils;
import fi.solita.utils.api.util.ResponseUtil;


public class HttpSerializationExceptionResolver implements HandlerExceptionResolver, Ordered {
    
    static String int2string(Integer i) {
        return Integer.toString(i);
    }

    @SuppressWarnings("unused")
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            for (HttpSerializers.InvalidRevisionException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidRevisionException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal 'revision'");
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidFilterException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidFilterException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal filter expression. Supported filters are: " + mkString(", ", e.validValues));
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidCountException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidCountException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal 'count'. Accepted values: " + mkString(", ", map(HttpSerializationExceptionResolver_.int2string, e.validValues)));
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidStartIndexException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidStartIndexException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal 'startIndex'. Must be a positive integer");
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidSRSNameException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidSRSNameException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal 'srsName'. Accepted values: " + mkString(", ", e.validValues));
                return new ModelAndView();
            }
            
            for (HttpSerializers.InvalidIntervalException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidIntervalException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal interval");
                return new ModelAndView();
            }
            for (HttpSerializers.BeginAndEndMustMatchException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.BeginAndEndMustMatchException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Unexpected interval. Only instants are accepted for the time being");
                return new ModelAndView();
            }
            for (HttpSerializers.IntervalNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.IntervalNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal interval. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
            
            for (HttpSerializers.InvalidLocalDateException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidLocalDateException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal localdate");
                return new ModelAndView();
            }
            for (HttpSerializers.LocalDateNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.LocalDateNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal localdate. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidLocalTimeException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidLocalTimeException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal localtime");
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidDurationException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidDurationException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal duration");
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidTimeZoneException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidTimeZoneException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal time zone");
                return new ModelAndView();
            }
            
            for (HttpSerializers.InvalidDateTimeException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidDateTimeException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal datetime");
                return new ModelAndView();
            }
            for (HttpSerializers.DateTimeNotWithinLimitsException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.DateTimeNotWithinLimitsException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal datetime. Accepted range: " + e.validStart + "-" + e.validEnd);
                return new ModelAndView();
            }
            for (HttpSerializers.InvalidURIException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidURIException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Illegal uri");
                return new ModelAndView();
            }
            
            for (HttpSerializers.InvalidEnumException e: ExceptionUtils.findCauseFromHierarchy(ex, HttpSerializers.InvalidEnumException.class)) {
                ResponseUtil.respondError(response, HttpStatus.BAD_REQUEST.value(), "Invalid " + e.name + ": " + e.value + ". Valid values: " + mkString(", ", e.validValues));
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
