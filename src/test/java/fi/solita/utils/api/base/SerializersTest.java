package fi.solita.utils.api.base;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SerializersTest {
    private Serializers serializers = new Serializers();
    
    @Test
    public void deserDateTime_acceptsZ() {
        assertNotNull(serializers.deserDateTime("2019-12-03T00:00:00Z"));
    }

    @Test
    public void deserDateTime_acceptsMillis() {
        assertNotNull(serializers.deserDateTime("2019-12-03T00:00:00.000Z"));
    }

    @Test
    public void deserDateTime_acceptsMicros() {
        assertNotNull(serializers.deserDateTime("2019-12-03T00:00:00.000000Z"));
    }
    
    @Test
    public void deserDateTime_acceptsOtherZone() {
        assertNotNull(serializers.deserDateTime("2019-12-03T00:00:00+02:00"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void deserDateTime_failsWithoutExplicitZone() {
        serializers.deserDateTime("2019-12-03T00:00:00");
    }
    
}
