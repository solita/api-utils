package fi.solita.utils.api.base;

import java.util.function.Function;

import org.junit.Test;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.types.Count;

public class CountConverterTest {

    @SuppressWarnings("unchecked")
    private Function<String,Count> c = (Function<String, Count>) new HttpSerializers(new Serializers()).converters().get(Count.class);
    
    @Test
    public void hyvaksyyValidinArvon() {
        c.apply(Integer.toString(10));
    }
    
    @Test(expected = HttpSerializers.InvalidValueException.class)
    public void eiHyvaksyMuutaArvoa() {
        c.apply(Integer.toString(11));
    }
}
