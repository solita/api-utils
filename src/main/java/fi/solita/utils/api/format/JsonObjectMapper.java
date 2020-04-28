package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Option.None;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.CalendarDeserializer;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.SqlDateDeserializer;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.TimestampDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.EnumSerializer;
import com.fasterxml.jackson.databind.ser.std.SqlDateSerializer;
import com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;

import fi.solita.utils.api.JsonDeserializeAsBean;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Option;

/**
 * Projektiperheen yleinen objectmapper. Rajoittaa Jacksonia tekemästä typeryyksiä.
 * Käytä tätä kaikkiin tarkoituksiin default-ObjectMapperin sijaan.
 *
 * Sovellusten sisäiseen liikenteeseen käytä InternalObjectMapper/InternalJsonSerializationService joka rekisteröityy automaattisesti Springille beaniksi.
 */
public class JsonObjectMapper extends ObjectMapper {

    public static class SerializingDeserializingProhibitedException extends RuntimeException {
        public SerializingDeserializingProhibitedException(String msg) {
            super(msg);
        }
    }

    public static class NoSerializerFoundException extends RuntimeException {
        public NoSerializerFoundException(Class<?> clazz) {
            super("Olisi tarvinnut sarjallistaa tyyppi " + clazz.getName() + ". Onhan tyypillä (ja sen sisällöillä) serializeri tai @" + JsonSerializeAsBean.class.getSimpleName() + "?");
        }
    }

    public static class NoDeserializerFoundException extends RuntimeException {
        public NoDeserializerFoundException(Class<?> clazz) {
            super("Olisi tarvinnut desarjallistaa tyyppi " + clazz.getName() + ". Onhan tyypillä (ja sen sisällöillä) deserializeri tai @" + JsonDeserializeAsBean.class.getSimpleName() + "?");
        }
    }
    
    private static class CustomBeanDeserializerFactory extends BeanDeserializerFactory {
        private CustomBeanDeserializerFactory(DeserializerFactoryConfig config) {
            super(config);
        }

        @Override
        public JsonDeserializer<?> createEnumDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
            JsonDeserializer<?> candidate = super.createEnumDeserializer(ctxt, type, beanDesc);
            if (candidate instanceof EnumDeserializer && !ClassUtils.getEnumType(type.getRawClass()).get().isAnnotationPresent(JsonDeserializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Enumille " + ClassUtils.getEnumType(type.getRawClass()).get().getName() + " ei ole rekisteröity deserializaria!");
            }

            return candidate;
        }
        
        @Override
        protected JsonDeserializer<?> findStdDeserializer(DeserializationContext ctxt, final JavaType type, BeanDescription beanDesc) throws JsonMappingException {
            if (type.getRawClass() == Option.class) {
                return new StdDeserializer<Option<?>>(type) {
                    @Override
                    public Option<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                        JsonDeserializer<?> valueDeser = findDeserializer(ctxt, type.containedType(0), null);
                        if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
                            return None();
                        }

                        return Option.of(valueDeser.deserialize(jp, ctxt));
                    }

                    @Override
                    public Option<?> getNullValue() {
                        return None();
                    }
                };
            }
            return super.findStdDeserializer(ctxt, type, beanDesc);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
            JsonDeserializer<?> candidate = super.createBeanDeserializer(ctxt, type, beanDesc);
            JsonDeserializer<?> actual = candidate;
            while (actual instanceof DelegatingDeserializer) {
                JsonDeserializer<?> delegatee = ((DelegatingDeserializer)actual).getDelegatee();
                if (delegatee == actual) {
                    break;
                }
                actual = delegatee;
            }
            if (actual instanceof CalendarDeserializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Calendaria!");
            } else if (actual instanceof DateDeserializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Datea!");
            } else if (actual instanceof TimestampDeserializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlTimestamppia!");
            } else if (actual instanceof SqlDateDeserializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlDatea!");
            } else if (actual instanceof BeanDeserializer && !type.getRawClass().isAnnotationPresent(JsonDeserializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Luokkaa " + type.getRawClass().getName() + " ei ole merkattu desarjallistettavaksi! (Unohditko @" + JsonDeserializeAsBean.class.getSimpleName() + "?)");
            }

            return (JsonDeserializer<Object>) candidate;
        }

        @Override
        public DeserializerFactory withConfig(DeserializerFactoryConfig config) {
            return new CustomBeanDeserializerFactory(config);
        }
    }

    private static class CustomBeanSerializerFactory extends BeanSerializerFactory {
        private CustomBeanSerializerFactory(SerializerFactoryConfig config) {
            super(config);
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType origType) throws JsonMappingException {
            JsonSerializer<?> candidate = super.createSerializer(prov, origType);
            if (candidate instanceof EnumSerializer && !ClassUtils.getEnumType(origType.getRawClass()).get().isAnnotationPresent(JsonSerializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Enumille " + ClassUtils.getEnumType(origType.getRawClass()).get().getName() + " ei ole rekisteröity serializaria!");
            } else if (candidate instanceof CalendarSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Calendaria!");
            } else if (candidate instanceof DateSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Datea!");
            } else if (candidate instanceof SqlTimeSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlTimea!");
            } else if (candidate instanceof SqlDateSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlDatea!");
            } else if (candidate instanceof BeanSerializer && !origType.getRawClass().isAnnotationPresent(JsonSerializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Luokkaa " + origType.getRawClass().getName() + " ei ole merkattu sarjallistettavaksi! (Unohditko @" + JsonSerializeAsBean.class.getSimpleName() + "?)");
            }

            return (JsonSerializer<Object>) candidate;
        }

        private static final StdKeySerializer stdKeySerializer = new StdKeySerializer();

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<Object> createKeySerializer(final SerializationConfig config, JavaType type, final JsonSerializer<Object> defaultImpl) {
            JsonSerializer<Object> serializer = super.createKeySerializer(config, type, defaultImpl);
            if (serializer == null) {
                return new StdSerializer<Object>((Class<Object>)type.getRawClass()) {
                    @Override
                    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                        JsonSerializer<Object> ser = createKeySerializer(config, SimpleType.construct(value.getClass()), defaultImpl);
                        if (value.getClass() == String.class) {
                            ser = stdKeySerializer;
                        } else if (ser == null || ser.getClass() == this.getClass()) {
                            throw new SerializingDeserializingProhibitedException("Ei KeySerializeria arvolle '" + value + "'. Anna Modulessa KeySerializer tyypille " + value.getClass());
                        }
                        ser.serialize(value, jgen, provider);
                    }
                };
            }
            return serializer;
        }

        @Override
        public SerializerFactory withConfig(SerializerFactoryConfig config) {
            return new CustomBeanSerializerFactory(config);
        }
    }

    public JsonObjectMapper() {
        super(null, null, new DefaultDeserializationContext.Impl(new CustomBeanDeserializerFactory(BeanDeserializerFactory.instance.getFactoryConfig())));

        setSerializerFactory(new CustomBeanSerializerFactory(BeanSerializerFactory.instance.getFactoryConfig()));

        // leave nulls out. This way we can restrict the serialized properties while using the same Dto.
        setSerializationInclusion(Include.NON_NULL);

        configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        configure(MapperFeature.AUTO_DETECT_SETTERS, false);
        configure(MapperFeature.AUTO_DETECT_CREATORS, false);
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); //TODO: pois
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false);
        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
    }

    @Override
    public boolean canDeserialize(JavaType type) {
        if (!super.canDeserialize(type)) {
            throw new NoDeserializerFoundException(type.getRawClass());
        }
        return true;
    }

    @Override
    public boolean canSerialize(Class<?> type) {
        if (!super.canSerialize(type) && type != void.class && type != Void.class) {
            throw new NoSerializerFoundException(type);
        }
        return true;
    }
}
