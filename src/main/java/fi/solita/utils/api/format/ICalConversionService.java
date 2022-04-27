package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.flatten;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import fi.solita.utils.api.base.ical.ICalModule;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;

public class ICalConversionService {

    private final ICalModule module;

    public ICalConversionService(ICalModule module) {
        this.module = module;
    }
    
    public byte[] serialize(HttpServletResponse res, String filename, Object obj) {
        return serialize(res, filename, newList(obj));
    }
    
    public byte[] serialize(HttpServletResponse res, String filename, final Map<?,? extends Iterable<?>> obj) {
        return serialize(res, filename, flatten(obj.values()));
    }
  
    public byte[] serialize(HttpServletResponse res, String filename, Object[] obj) {
        return serialize(res, filename, newList(obj));
    }
    
    public byte[] serialize(HttpServletResponse res, String filename, Iterable<?> obj) {
        Calendar calendar = new Calendar();
        //calendar.getProperties().add(new ProdId("-//Fintraffic//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        
        for (Object o: obj) {
            calendar.getComponents().add(module.serialize(o));
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        CalendarOutputter outputter = new CalendarOutputter();
        try {
            outputter.output((Calendar)calendar, out);
        } catch (ValidationException | IOException e) {
            throw new RuntimeException();
        }
        
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".ics");
        return out.toByteArray();
    }
}
