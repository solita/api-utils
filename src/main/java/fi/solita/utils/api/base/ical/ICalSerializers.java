package fi.solita.utils.api.base.ical;

import static fi.solita.utils.functional.Collections.emptyMap;

import java.util.Map;

import fi.solita.utils.api.base.Serializers;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

public class ICalSerializers {
    
    protected static final TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    
    @SuppressWarnings("unused")
    private final Serializers s;
    
    public ICalSerializers(Serializers s) {
        this.s = s;
    }
    
    public Map<Class<?>, ICalSerializer<?>> serializers() { return emptyMap(); }
}
