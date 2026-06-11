package fi.solita.utils.api.format;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.json.JsonModule;
import fi.solita.utils.api.base.json.JsonSerializers;
import fi.solita.utils.functional.Option;

public class JsonObjectMapperTest {

    @JsonSerializeAsBean
    public static final class TestDto {
        public final Option<String> optional;

        public TestDto(Option<String> optional) {
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
        String serialized = new String(json().serialize(new TestDto(Option.None())));

        assertEquals("{}", serialized);
    }

    @Test
    public void optionSomeFieldIsSerializedNormally() {
        String serialized = new String(json().serialize(new TestDto(Option.Some("value"))));

        assertEquals("{\"optional\":\"value\"}", serialized);
    }
}
