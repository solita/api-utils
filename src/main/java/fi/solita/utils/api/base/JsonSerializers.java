package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.sequence;
import static fi.solita.utils.functional.Functional.zip;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import fi.solita.utils.api.ResolvedMember;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaField;

/**
 * Base serializers/deserializers for the first API version.
 * These must not change! Changes should happen relative to previous api versions.
 */
public class JsonSerializers {
    
    private final Serializers s;
    
    public JsonSerializers(Serializers s) {
        this.s = s;
    }
    
    public static final <T> StdSerializer<T> stringSerializer(final Apply<T,String> f, Class<T> clazz) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeString(f.apply(value));
            }
        };
    }
    
    public static final <T> StdDeserializer<T> stringDeserializer(final Apply<String,T> f, Class<T> clazz) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getValueAsString());
            }
        };
    }
    
    public static final <T> StdSerializer<T> intSerializer(final Apply<T,Integer> f, Class<T> clazz) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    
    public static final <T> StdSerializer<T> longSerializer(final Apply<T,Long> f, Class<T> clazz) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    
    public static final <T> StdSerializer<T> delegateSerializer(final Apply<T,?> f, Class<T> clazz) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeObject(f.apply(value));
            }
        };
    }
    
    public static <T> Map<String,Object> toMap(T value, Iterable<? extends MetaField<? super T, ?>> fields) {
        return newMap(zip(map(JsonSerializers_.fieldName, fields), sequence(value, fields)));
    }

    static String fieldName(MetaField<?,?> field) {
        return field.getName();
    }
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> resolvedMember = Pair.of(ResolvedMember.class, new StdSerializer<ResolvedMember>(ResolvedMember.class) {
        @Override
        public void serialize(ResolvedMember value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeRawValue(new String(value.getData(), Charset.forName("UTF-8")));
        }
    });
    
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
    
    public final Map.Entry<? extends Class<?>, ? extends JsonSerializer<?>> interval = Pair.of(Interval.class, new StdSerializer<Interval>(Interval.class) {
        @Override
        public void serialize(Interval value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            Pair<DateTime,DateTime> i = s.ser(value);
            jgen.writeString(s.ser(i.left()) + "/" + s.ser(i.right()));
        }
    });
    
    public Map<Class<?>,JsonSerializer<?>> serializers() { return newMap(
            resolvedMember,
            option,
            either,
            tuple,
            Pair.of(URI.class, stringSerializer(Serializers_.ser.ap(s), URI.class)),
            Pair.of(LocalDate.class, stringSerializer(Serializers_.ser1.ap(s), LocalDate.class)),
            Pair.of(LocalTime.class, stringSerializer(Serializers_.ser2.ap(s), LocalTime.class)),
            Pair.of(DateTime.class, stringSerializer(Serializers_.ser3.ap(s), DateTime.class)),
            interval,
            Pair.of(Duration.class, stringSerializer(Serializers_.ser5.ap(s), Duration.class)),
            Pair.of(DateTimeZone.class, stringSerializer(Serializers_.ser6.ap(s), DateTimeZone.class))
        );
    }
    
    public Map<Class<?>,JsonSerializer<?>> keySerializers() { return emptyMap(); }
    
    public Map<Class<?>,JsonDeserializer<?>> deserializers() { return newMap(
            Pair.<Class<?>,JsonDeserializer<?>>of(DateTime.class, stringDeserializer(Serializers_.deserDateTime.ap(s), DateTime.class))
        );
    }
    
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

