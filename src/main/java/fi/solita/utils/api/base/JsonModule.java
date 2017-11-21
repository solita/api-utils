package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Option.None;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import fi.solita.utils.api.ClassUtils;
import fi.solita.utils.api.format.JsonObjectMapper.SerializingDeserializingProhibitedException;
import fi.solita.utils.functional.Option;

public class JsonModule extends SimpleModule {

    public final Map<Class<?>,Class<?>> rawTypes;

    @SuppressWarnings("unchecked")
    public JsonModule(Map<Class<?>, JsonSerializer<?>> serializers, Map<Class<?>, JsonSerializer<?>> keySerializers, Map<Class<?>, JsonDeserializer<?>> deserializers, Map<Class<?>,Class<?>> rawTypes) {
        for (Map.Entry<Class<?>, JsonSerializer<?>> s: serializers.entrySet()) {
            this.addSerializer((Class<Object>)s.getKey(), (JsonSerializer<Object>)s.getValue());
        }
        for (Map.Entry<Class<?>, JsonSerializer<?>> s: keySerializers.entrySet()) {
            this.addKeySerializer((Class<Object>)s.getKey(), (JsonSerializer<Object>)s.getValue());
        }
        for (Map.Entry<Class<?>, JsonDeserializer<?>> s: deserializers.entrySet()) {
            this.addDeserializer((Class<Object>)s.getKey(), (JsonDeserializer<Object>)s.getValue());
        }
        this.rawTypes = rawTypes;
    }
    
    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addDeserializers(new DeserializersBase());
        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                final JsonDeserializer<?> des = super.modifyDeserializer(config, beanDesc, deserializer);
                return new Delegater(des);
            }
        });

        addSerializer(new StdSerializer<Option<?>>(Option.class, false) {
            @Override
            public void serialize(Option<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
                if (value.isDefined()) {
                    jgen.writeObject(value.get());
                } else {
                    jgen.writeNull();
                }
            }
        });
    }
    
    private static class DeserializersBase extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer(final JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
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
            return null;
        }
    }

    private static final class Delegater extends DelegatingDeserializer {
        public Delegater(JsonDeserializer<?> delegatee) {
            super(delegatee);
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
            return new Delegater(newDelegatee);
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object ret = super.deserialize(jp, ctxt);
            for (Field f: ClassUtils.AllDeclaredApplicationFields.apply(ret.getClass())) {
                try {
                    f.setAccessible(true);
                    if (f.get(ret) == null) {
                        if (Option.class.isAssignableFrom(f.getType())) {
                            // set missing Option:s to None()
                            f.set(ret, Option.None());
                        } else {
                            throw new CannotDeserializeDueToMissingFieldException(f.getName());
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return ret;
        }
    }
    
    private static class CannotDeserializeDueToMissingFieldException extends SerializingDeserializingProhibitedException {
        public CannotDeserializeDueToMissingFieldException(String fieldName) {
            super("Field '" + fieldName + "' was left null since it was missing from the input data.");
        }
    }
}
