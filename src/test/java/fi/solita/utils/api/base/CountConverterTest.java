package fi.solita.utils.api.base;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.types.Count;

public class CountConverterTest {

    private Converter<String,Count> c = new HttpSerializers(new Serializers()).count.getValue();
    
    @Test
    public void hyvaksyyValidinArvon() {
        c.convert(Integer.toString(10));
    }
    
    @Test(expected = HttpSerializers.InvalidCountException.class)
    public void eiHyvaksyMuutaArvoa() {
        c.convert(Integer.toString(11));
    }
}
