package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newMap;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple;

/**
 * Base serializers/deserializers for the first API version.
 * These must not change! Changes should happen relative to previous api versions.
 */
public class JsonSerializers {
    
    private final Serializers s;
    
    public JsonSerializers(Serializers s) {
        this.s = s;
    }
    
    @SuppressWarnings("rawtypes")
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> option = Pair.of(Option.class, new StdSerializer<Option>(Option.class) {
        @Override
        public void serialize(Option value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            if (value.isDefined()) {
                jgen.writeObject(value.get());
            } else {
                jgen.writeNull();
            }
        }
    });
    
    @SuppressWarnings("rawtypes")
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> either = Pair.of(Either.class, new StdSerializer<Either>(Either.class) {
        @Override
        public void serialize(Either value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            if (value.isLeft()) {
                jgen.writeObject(value.left.get());
            } else {
                jgen.writeObject(value.right.get());
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> tuple = Pair.of(Tuple.class, new StdSerializer<Tuple>(Tuple.class) {
        @Override
        public void serialize(Tuple value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeStartArray();
            for (Object o: value.toArray()) {
                jgen.writeObject(o);
            }
            jgen.writeEndArray();
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> uri = Pair.of(URI.class, new StdSerializer<URI>(URI.class) {
        @Override
        public void serialize(URI value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> localdate = Pair.of(LocalDate.class, new StdSerializer<LocalDate>(LocalDate.class) {
        @Override
        public void serialize(LocalDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> localtime = Pair.of(LocalTime.class, new StdSerializer<LocalTime>(LocalTime.class) {
        @Override
        public void serialize(LocalTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> datetime = Pair.of(DateTime.class, new StdSerializer<DateTime>(DateTime.class) {
        @Override
        public void serialize(DateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> interval = Pair.of(Interval.class, new StdSerializer<Interval>(Interval.class) {
        @Override
        public void serialize(Interval value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            Pair<DateTime,DateTime> i = s.ser(value);
            jgen.writeString(s.ser(i.left) + "/" + s.ser(i.right));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> duration = Pair.of(Duration.class, new StdSerializer<Duration>(Duration.class) {
        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> datetimezone = Pair.of(DateTimeZone.class, new StdSerializer<DateTimeZone>(DateTimeZone.class) {
        @Override
        public void serialize(DateTimeZone value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeString(s.ser(value));
        }
    });
    
    
            
    
    public Map<Class<?>,JsonSerializer<?>> serializers() { return newMap(
            option,
            either,
            tuple,
            uri,
            localdate,
            localtime,
            datetime,
            interval,
            duration,
            datetimezone
        );
    }
    public Map<Class<?>,JsonSerializer<?>> keySerializers() { return emptyMap(); }
    public Map<Class<?>,JsonDeserializer<?>> deserializers() { return emptyMap(); }
    
    // Näiden pitäisi vastata sarjallistusten lopullisia raakatyyppejä
    public Map<Class<?>,Class<?>> rawTypes() { return Collections.<Class<?>,Class<?>>newMap(
            // tuple?
            Pair.of(URI.class, String.class),
            Pair.of(LocalDate.class, String.class),
            Pair.of(LocalTime.class, String.class),
            Pair.of(DateTime.class, String.class),
            Pair.of(Interval.class, String.class),
            Pair.of(Duration.class, String.class),
            Pair.of(DateTimeZone.class, String.class)
            );
    }
}

