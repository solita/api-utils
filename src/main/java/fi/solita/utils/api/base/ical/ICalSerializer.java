package fi.solita.utils.api.base.ical;

import net.fortuna.ical4j.model.component.CalendarComponent;

public abstract class ICalSerializer<T> {

    public abstract CalendarComponent serialize(ICalModule module, T value);
    
}