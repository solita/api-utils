package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.flatten;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import fi.solita.utils.api.base.ical.ICalModule;
import fi.solita.utils.api.util.RequestUtil;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.RefreshInterval;
import net.fortuna.ical4j.model.property.Source;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;
import net.fortuna.ical4j.validate.ValidationResult;

public class ICalConversionService {

    private final ICalModule module;

    public ICalConversionService(ICalModule module) {
        this.module = module;
    }
    
    public byte[] serialize(HttpServletRequest req, HttpServletResponse res, String filename, Object obj) {
        return serialize(req, res, filename, newList(obj));
    }
    
    public byte[] serialize(HttpServletRequest req, HttpServletResponse res, String filename, final Map<?,? extends Iterable<?>> obj) {
        return serialize(req, res, filename, flatten(obj.values()));
    }
  
    public byte[] serialize(HttpServletRequest req, HttpServletResponse res, String filename, Object[] obj) {
        return serialize(req, res, filename, newList(obj));
    }
    
    protected URI getCalendarUri(HttpServletRequest req, URI uri) {
        return uri;
    }
    
    protected URI makeObjectURI(HttpServletRequest req, String objectURLProperty) {
        String apiVersionBasePath = RequestUtil.getApiVersionBasePath(req);
        return URI.create(apiVersionBasePath + objectURLProperty + ".html");
    }
    
    protected Calendar createCalendar() {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        calendar.getProperties().add(new RefreshInterval(new ParameterList(), "PT10M"));
        return calendar;
    }
    
    public byte[] serialize(HttpServletRequest req, HttpServletResponse res, String filename, Iterable<?> obj) {
        Calendar calendar = createCalendar();
        calendar.getProperties().add(new Source(new ParameterList(), getCalendarUri(req, RequestUtil.getRequestURI(req)).toString()));
        
        for (Object o: obj) {
            CalendarComponent component = module.serialize(o);
            Url urlProp = (Url)component.getProperty(Property.URL);
            if (urlProp != null) {
                urlProp.setUri(makeObjectURI(req, urlProp.getValue()));
            }
            calendar.getComponents().add(component);
            
            ValidationResult validationResult = component.validate();
            if (validationResult.hasErrors()) {
                calendar.getComponents().clear();
                calendar.getComponents().add(component);
                String str = outputCalendar(calendar).toString(Charset.forName("UTF-8"));
                throw new RuntimeException(validationResult.toString() + ":\n" + str);
            }
        }
        
        ByteArrayOutputStream out = outputCalendar(calendar);
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".ics");
        return out.toByteArray();
    }

    protected ByteArrayOutputStream outputCalendar(Calendar calendar) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        try {
            outputter.output((Calendar)calendar, out);
        } catch (ValidationException | IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
