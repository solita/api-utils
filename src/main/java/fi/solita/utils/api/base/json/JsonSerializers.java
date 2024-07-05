package fi.solita.utils.api.base.json;

import static fi.solita.utils.functional.Collections.it;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.sequence;
import static fi.solita.utils.functional.Functional.zip;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;

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

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.Serializers_;
import fi.solita.utils.api.resolving.ResolvedMember;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.SemiGroups;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

/**
 * Base serializers/deserializers for the first API version.
 * These must not change! Changes should happen relative to previous api versions.
 */
public class JsonSerializers {
    
    private final Serializers s;
    
    public JsonSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    
    
    /**
     * Some primitive serializers, to be used as helper functions for actual serialization
     */
    
    public static final <T> JsonSerializer<T> keySerializer(Class<T> clazz, final Apply<T,String> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeFieldName(f.apply(value));
            }
        };
    }
    
    public static final <T> JsonSerializer<T> charSerializer(Class<T> clazz, final Apply<T,Character> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeString(Character.toString(f.apply(value)));
            }
        };
    }
    public static final <T> JsonDeserializer<T> charDeserializer(Class<T> clazz, final Apply<Character,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(Assert.singleton(it(p.getText())));
            }
        };
    }
    
    public static final <T> JsonSerializer<T> stringSerializer(Class<T> clazz, final Apply<T,String> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeString(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> stringDeserializer(Class<T> clazz, final Apply<String,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getText());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> shortSerializer(Class<T> clazz, final Apply<T,Short> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> shortDeserializer(Class<T> clazz, final Apply<Short,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getShortValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> intSerializer(Class<T> clazz, final Apply<T,Integer> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> intDeserializer(Class<T> clazz, final Apply<Integer,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getIntValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> longSerializer(Class<T> clazz, final Apply<T,Long> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> longDeserializer(Class<T> clazz, final Apply<Long,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getLongValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> bigIntegerSerializer(Class<T> clazz, final Apply<T,BigInteger> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> bigIntegerDeserializer(Class<T> clazz, final Apply<BigInteger,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getBigIntegerValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> bigDecimalSerializer(Class<T> clazz, final Apply<T,BigDecimal> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeNumber(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> bigDecimalDeserializer(Class<T> clazz, final Apply<BigDecimal,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getDecimalValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> booleanSerializer(Class<T> clazz, final Apply<T,Boolean> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeBoolean(f.apply(value));
            }
        };
    }
    public static final <T> JsonDeserializer<T> booleanDeserializer(Class<T> clazz, final Apply<Boolean,T> f) {
        return new StdDeserializer<T>(Boolean.class) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.getBooleanValue());
            }
        };
    }
    
    public static final <T> JsonSerializer<T> delegateSerializer(Class<T> clazz, final Apply<T,?> f) {
        return new StdSerializer<T>(clazz) {
            @Override
            public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                jgen.writeObject(f.apply(value));
            }
        };
    }
    public static final <D,T> JsonDeserializer<T> delegateDeserializer(final Class<T> clazz, final Class<D> delegate, final Apply<D,T> f) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return f.apply(p.readValueAs(delegate));
            }
        };
    }
    
    public static final <T extends Enum<T>> JsonDeserializer<T> enumDeserializer(final Class<T> clazz, final Apply<T,String> serializer) {
        return new StdDeserializer<T>(clazz) {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                String value = p.getValueAsString();
                for (T t: clazz.getEnumConstants()) {
                    if (serializer.apply(t).equals(value)) {
                        return t;
                    }
                }
                throw new RuntimeException("Cannot deserialize " + value);
            }
        };
    }
    
    
    
    
    
    /**
     * Some helper functions
     */
    
    public static <T> Map<String,Object> toMap(T value, Iterable<? extends MetaNamedMember<T, ?>> fields) {
        return newMap(SemiGroups.fail(), zip(map(JsonSerializers_.fieldName, fields), sequence(value, fields)));
    }

    static String fieldName(MetaNamedMember<?,?> field) {
        return field.getName();
    }
    
    
    
    
    
    /**
     * Some concrete serializers for common types
     */
    
    private final JsonSerializer<ResolvedMember> resolvedMember = new StdSerializer<ResolvedMember>(ResolvedMember.class) {
        @Override
        public void serialize(ResolvedMember value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeRawValue(new String(value.getData(), Charset.forName("UTF-8")));
        }
    };
    
    @SuppressWarnings("rawtypes")
    private final JsonSerializer<Option> option = new StdSerializer<Option>(Option.class) {
        @Override
        public void serialize(Option value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            if (value.isDefined()) {
                jgen.writeObject(value.get());
            } else {
                jgen.writeNull();
            }
        }
    };
    
    @SuppressWarnings("rawtypes")
    private final JsonSerializer<Either> either = new StdSerializer<Either>(Either.class) {
        @Override
        public void serialize(Either value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            if (value.isLeft()) {
                jgen.writeObject(value.left.get());
            } else {
                jgen.writeObject(value.right.get());
            }
        }
    };
    
    private final JsonSerializer<?> tuple = new StdSerializer<Tuple>(Tuple.class) {
        @Override
        public void serialize(Tuple value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeStartArray();
            for (Object o: value.toArray()) {
                jgen.writeObject(o);
            }
            jgen.writeEndArray();
        }
    };
    
    private final JsonSerializer<?> interval = new StdSerializer<Interval>(Interval.class) {
        @Override
        public void serialize(Interval value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            Pair<DateTime,DateTime> i = s.ser(value);
            jgen.writeString(s.ser(i.left()) + "/" + s.ser(i.right()));
        }
    };
    
    @SuppressWarnings("rawtypes")
    private final JsonSerializer<Option> optionKey = new StdSerializer<Option>(Option.class) {
        @Override
        public void serialize(Option value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            if (value.isDefined()) {
                provider.findKeySerializer(value.get().getClass(), null).serialize(value.get(), jgen, provider);
            } else {
                jgen.writeFieldName("");
            }
        }
    };
    
    
    
    
    public Map<Class<?>,JsonSerializer<?>> serializers() { return newMap(
        Pair.of(ResolvedMember.class, resolvedMember),
        Pair.of(Option.class, option),
        Pair.of(Either.class, either),
        Pair.of(Tuple.class, tuple),
        Pair.of(URI.class, stringSerializer(URI.class, Serializers_.ser.ap(s))),
        Pair.of(UUID.class, stringSerializer(UUID.class, Serializers_.ser9.ap(s))),
        Pair.of(LocalDate.class, stringSerializer(LocalDate.class, Serializers_.ser1.ap(s))),
        Pair.of(LocalTime.class, stringSerializer(LocalTime.class, Serializers_.ser2.ap(s))),
        Pair.of(DateTime.class, stringSerializer(DateTime.class, Serializers_.ser3.ap(s))),
        Pair.of(Interval.class, interval),
        Pair.of(Duration.class, stringSerializer(Duration.class, Serializers_.ser5.ap(s))),
        Pair.of(Period.class, stringSerializer(Period.class, Serializers_.ser6.ap(s))),
        Pair.of(DateTimeZone.class, stringSerializer(DateTimeZone.class, Serializers_.ser7.ap(s)))
    );
    }
    
    public Map<Class<?>,JsonSerializer<?>> keySerializers() { return newMap(
        Pair.of(Option.class, optionKey));
    }
    
    public Map<Class<?>,JsonDeserializer<?>> deserializers() { return newMap(
            Pair.<Class<?>,JsonDeserializer<?>>of(DateTime.class, stringDeserializer(DateTime.class, Serializers_.deserDateTime.ap(s))),
            Pair.<Class<?>,JsonDeserializer<?>>of(LocalDate.class, stringDeserializer(LocalDate.class, Serializers_.deserLocalDate.ap(s))),
            Pair.<Class<?>,JsonDeserializer<?>>of(LocalTime.class, stringDeserializer(LocalTime.class, Serializers_.deserLocalTime.ap(s)))
        );
    }
    
    // Näiden pitäisi vastata sarjallistusten lopullisia raakatyyppejä.
    // Tähän pitää laittaa sellaiset tyypit, jotka haluaa sarjallistettavan suoraan jonkin toisen tyypin kaltaisena.
    // Toteutus varmistaa, että nämä sitten toimivat myös generics-luokkien sisälle käärittyinä (mihin pelkkä direcModelSubstitutes ei riitä).
    public Map<Class<?>,Class<?>> rawTypes() { return Collections.<Class<?>,Class<?>>newMap(
        Pair.of(Character.class, String.class),
        Pair.of(LocalDate.class, java.sql.Date.class),
        Pair.of(DateTime.class, java.util.Date.class),
        Pair.of(PropertyName.class, String.class)
    );
    }
}

