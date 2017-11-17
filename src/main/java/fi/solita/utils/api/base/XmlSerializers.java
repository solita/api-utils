package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.newMap;

import java.net.URI;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class XmlSerializers {

    private final Serializers s;

    public XmlSerializers(Serializers s) {
        this.s = s;
    }
    
    public Map<Class<?>, XmlAdapter<?,?>> adapters() { return
            (Map<Class<?>, XmlAdapter<?,?>>)(Object)newMap(
                    Pair.of(Option.class, new OptionAdapter<Object>().with(s)),
                    // LongId and StringId must be specified for each id individually,
                    Pair.of(LocalDate.class, new LocalDateAdapter().with(s)),
                    Pair.of(LocalTime.class, new LocalTimeAdapter().with(s)),
                    Pair.of(DateTime.class, new DateTimeAdapter().with(s)),
                    Pair.of(Interval.class, new IntervalAdapter().with(s)),
                    Pair.of(Duration.class, new DurationAdapter().with(s)),
                    Pair.of(DateTimeZone.class, new DateTimeZoneAdapter().with(s))
                );
    }
    
    public static final class OptionAdapter<T> extends XmlAdapterBase<Serializers,T,Option<T>> {
        @Override
        public T marshal(Option<T> v) throws Exception {
            return v.getOrElse(null);
        }

        @Override
        public Option<T> unmarshal(T v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
    
    public static final class URIAdapter extends XmlAdapterBase<Serializers,String,URI> {
        @Override
        public String marshal(URI v) throws Exception {
            return s().ser(v);
        }

        @Override
        public URI unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
        
    }
    
    public static final class LocalDateAdapter extends XmlAdapterBase<Serializers,String,LocalDate> {
        @Override
        public String marshal(LocalDate v) throws Exception {
            return s().ser(v);
        }

        @Override
        public LocalDate unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    public static final class LocalTimeAdapter extends XmlAdapterBase<Serializers,String,LocalTime> {
        @Override
        public String marshal(LocalTime v) throws Exception {
            return s().ser(v);
        }

        @Override
        public LocalTime unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    public static final class DateTimeAdapter extends XmlAdapterBase<Serializers,String,DateTime> {
        @Override
        public String marshal(DateTime v) throws Exception {
            return s().ser(v);
        }

        @Override
        public DateTime unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    public static final class IntervalAdapter extends XmlAdapterBase<Serializers,IntervalAdapter.IntervalWrapper,Interval> {
        public static class IntervalWrapper {
            @XmlJavaTypeAdapter(DateTimeAdapter.class)
            public DateTime alku;
            @XmlJavaTypeAdapter(DateTimeAdapter.class)
            public DateTime loppu;
            
            public IntervalWrapper(Pair<DateTime,DateTime> pair) {
                this.alku = pair.left;
                this.loppu = pair.right;
            }
            IntervalWrapper() {
            }
        }

        @Override
        public IntervalWrapper marshal(Interval v) throws Exception {
            return new IntervalWrapper(s().ser(v));
        }

        @Override
        public Interval unmarshal(IntervalWrapper v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    public static final class DurationAdapter extends XmlAdapterBase<Serializers,String,Duration> {
        @Override
        public String marshal(Duration v) throws Exception {
            return s().ser(v);
        }

        @Override
        public Duration unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    public static final class DateTimeZoneAdapter extends XmlAdapterBase<Serializers,String,DateTimeZone> {
        @Override
        public String marshal(DateTimeZone v) throws Exception {
            return s().ser(v);
        }

        @Override
        public DateTimeZone unmarshal(String v) throws Exception {
            throw new UnsupportedOperationException("Not implemented");
        }
    };
    
    
}
