package fi.solita.utils.api.format;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
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
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.*;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Default;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Dynamic;
import fi.solita.utils.api.JsonDeserializeAsBean;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.functional.Option;

import java.io.IOException;

import static fi.solita.utils.functional.Option.None;

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
        private final boolean checkJsonSerializeAsBean;
        private CustomBeanSerializerFactory(boolean checkJsonSerializeAsBean, SerializerFactoryConfig config) {
            super(config);
            this.checkJsonSerializeAsBean = checkJsonSerializeAsBean;
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType origType) throws JsonMappingException {
            JsonSerializer<?> candidate = super.createSerializer(prov, origType);
            if (candidate instanceof Dynamic || candidate instanceof Default) {
                throw new SerializingDeserializingProhibitedException("Tyypille " + origType.getRawClass().getName() + " ei ole rekisteröity serializaria!");
            } else if (candidate instanceof EnumSerializer && !ClassUtils.getEnumType(origType.getRawClass()).get().isAnnotationPresent(JsonSerializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Enumille " + ClassUtils.getEnumType(origType.getRawClass()).get().getName() + " ei ole rekisteröity serializaria!");
            } else if (candidate instanceof CalendarSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Calendaria!");
            } else if (candidate instanceof DateSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää Datea!");
            } else if (candidate instanceof SqlTimeSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlTimea!");
            } else if (candidate instanceof SqlDateSerializer) {
                throw new SerializingDeserializingProhibitedException("Ei pitäisi käyttää SqlDatea!");
            } else if (checkJsonSerializeAsBean && candidate instanceof BeanSerializer && !origType.getRawClass().isAnnotationPresent(JsonSerializeAsBean.class)) {
                throw new SerializingDeserializingProhibitedException("Luokkaa " + origType.getRawClass().getName() + " ei ole merkattu sarjallistettavaksi! (Unohditko @" + JsonSerializeAsBean.class.getSimpleName() + "?)");
            }

            return (JsonSerializer<Object>) candidate;
        }
        
        private static final JsonSerializer<Object> stdStringKeySerializer = StdKeySerializers.getStdKeySerializer(null, String.class, false);

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<Object> createKeySerializer(final SerializerProvider ctxt, JavaType type, final JsonSerializer<Object> defaultImpl) throws JsonMappingException {
            JsonSerializer<Object> serializer = super.createKeySerializer(ctxt, type, defaultImpl);
            if (serializer == null || serializer instanceof Dynamic || serializer instanceof Default) {
                return new StdSerializer<Object>((Class<Object>)type.getRawClass()) {
                    @Override
                    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                        JsonSerializer<Object> ser = createKeySerializer(ctxt, provider.getTypeFactory().constructType(value.getClass()), defaultImpl);
                        if (value.getClass() == String.class) {
                            ser = stdStringKeySerializer;
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
            return new CustomBeanSerializerFactory(checkJsonSerializeAsBean, config);
        }
    }

    public JsonObjectMapper(boolean checkJsonSerializeAsBean) {
        super(null, null, new DefaultDeserializationContext.Impl(new CustomBeanDeserializerFactory(BeanDeserializerFactory.instance.getFactoryConfig())));

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
        configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        
        // ugh...
        setSerializerFactory(new CustomBeanSerializerFactory(checkJsonSerializeAsBean, BeanSerializerFactory.instance.getFactoryConfig()));
        setSerializerProvider(new DefaultSerializerProvider.Impl().createInstance(getSerializationConfig(), getSerializerFactory()));
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
