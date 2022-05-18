package fi.solita.utils.api.base.ical;

import java.util.Map;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.functional.Pair;
import static fi.solita.utils.functional.Collections.*;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;

public class ICalSerializers {
    
    protected static final TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    
    @SuppressWarnings("unused")
    private final Serializers s;
    
    public ICalSerializers(Serializers s) {
        this.s = s;
    }
    
    public static final <T> ICalSerializer<T> beanSerializer() {
		return new ICalSerializer<T>() {
			@Override
			public CalendarComponent serialize(ICalModule module, T value) {
				return new VEvent(false);
			}
		};
    }
    
    public Map<Class<?>, ICalSerializer<?>> serializers() {
    	return newMap(
			Pair.<Class<?>,ICalSerializer<?>>of(JsonSerializeAsBean.class, beanSerializer())
		);
	}
}
