package fi.solita.utils.api.base;

import static fi.solita.utils.functional.FunctionalC.takeWhile;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.net.URI;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Pair;

/**
 * Base serializers/deserializers for the first API version.
 * These must not change! Changes should happen relative to previous api versions.
 * Use inheritance to evolve these.
 */
public class Serializers {

    public static final DateTimeFormatter DATETIME_FORMAT_WITH_MILLIS = ISODateTimeFormat.dateTime().withZoneUTC();
    public static final DateTimeFormatter DATETIME_FORMAT_NO_MILLIS = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
    public static final DateTimeZone APP_ZONE = DateTimeZone.forID("Europe/Helsinki");
    
    // Pakko käyttää sarjallistusluokkia, sillä Jacksonille ei tunnu pystyvän muuten kertomaan rakennetta.
    
     @JsonSerializeAsBean
     public static final class RevisioJaMuokkausaikaData {
         public final Long revisio;
         public final DateTime muokkaus_aika;
         public RevisioJaMuokkausaikaData(Long revisio, DateTime muokkaus_aika) {
             this.revisio = revisio;
             this.muokkaus_aika = muokkaus_aika;
         }
     }
     
    public String ser(URI v) {
        return v.toString();
    }
    
    public String ser(LocalDate v) {
        return v.toString();
    }
    
    public LocalDate deserLocalDate(String value) {
        return LocalDate.parse(value, ISODateTimeFormat.date());
    }
    
    public String ser(LocalTime v) {
        return v.toString(ISODateTimeFormat.timeNoMillis().withZoneUTC());
    }
    
    public LocalTime deserLocalTime(String value) {
        return LocalTime.parse(value);
    }
    
    public String ser(DateTime v) {
        return v.toString(DATETIME_FORMAT_NO_MILLIS);
    }
    
    public DateTime deserDateTime(String value) {
        return DateTime.parse(value, value.contains(".") ? DATETIME_FORMAT_WITH_MILLIS : DATETIME_FORMAT_NO_MILLIS);
    }
    
    public String serZoned(DateTime value) {
        return takeWhile(not(equalTo('+')), value.toString(ISODateTimeFormat.dateTimeNoMillis().withZone(APP_ZONE))).replace('T', ' ');
    }
    
    public Pair<DateTime,DateTime> ser(Interval v) {
        return Pair.of(v.getStart(), v.getEnd());
    }
    
    public Interval deserInterval(String value) {
        String[] parts = value.split("/");
        return new Interval(deserDateTime(parts[0]), deserDateTime(parts[1]));
    }
    
    public String ser(Duration v) {
        return v.toString();
    }
    
    public String ser(Period v) {
        return v.toString();
    }
    
    public String ser(DateTimeZone v) {
        return v.getID();
    }
    
    public RevisioJaMuokkausaikaData ser(Long revisio, DateTime muokkaus_aika) {
            return new RevisioJaMuokkausaikaData(revisio, muokkaus_aika);
    }
    
    public String ser(UUID v) {
        return v.toString();
    }
}
