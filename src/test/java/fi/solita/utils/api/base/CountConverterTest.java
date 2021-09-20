package fi.solita.utils.api.base;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.types.Count;

public class CountConverterTest {

    @SuppressWarnings("unchecked")
    private Converter<String,Count> c = (Converter<String, Count>) new HttpSerializers(new Serializers()).converters().get(Count.class);
    
    @Test
    public void hyvaksyyValidinArvon() {
        c.convert(Integer.toString(10));
    }
    
    @Test(expected = HttpSerializers.InvalidValueException.class)
    public void eiHyvaksyMuutaArvoa() {
        c.convert(Integer.toString(11));
    }
}
