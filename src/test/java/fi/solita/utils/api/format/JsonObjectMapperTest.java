package fi.solita.utils.api.format;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.json.JsonModule;
import fi.solita.utils.api.base.json.JsonSerializers;
import java.util.Optional;

public class JsonObjectMapperTest {

    @JsonSerializeAsBean
    public static final class TestDto {
        public final Optional<String> optional;

        public TestDto(Optional<String> optional) {
            this.optional = optional;
        }
    }

    private JsonConversionService json() {
        JsonSerializers jsonSerializers = new JsonSerializers(new Serializers());
        return new JsonConversionService(
            new JsonObjectMapper(true),
            new JsonModule(
                jsonSerializers.serializers(),
                jsonSerializers.keySerializers(),
                jsonSerializers.deserializers(),
                jsonSerializers.rawTypes()
            )
        );
    }

    @Test
    public void optionNoneFieldIsOmitted() {
        String serialized = new String(json().serialize(new TestDto(Optional.empty())));

        assertEquals("{}", serialized);
    }

    @Test
    public void optionSomeFieldIsSerializedNormally() {
        String serialized = new String(json().serialize(new TestDto(Optional.of("value"))));

        assertEquals("{\"optional\":\"value\"}", serialized);
    }
}
