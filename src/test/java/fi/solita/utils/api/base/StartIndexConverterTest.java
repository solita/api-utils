package fi.solita.utils.api.base;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.types.StartIndex;

public class StartIndexConverterTest {

    @SuppressWarnings("unchecked")
    private Converter<String,StartIndex> c = (Converter<String, StartIndex>) new HttpSerializers(new Serializers()).converters().get(StartIndex.class);
    
    @Test
    public void hyvaksyyValidinArvon() {
        c.convert(Integer.toString(10));
    }
    
    @Test(expected = HttpSerializers.InvalidStartIndexException.class)
    public void eiHyvaksyNollaa() {
        c.convert(Integer.toString(0));
    }
    
    @Test(expected = HttpSerializers.InvalidStartIndexException.class)
    public void eiHyvaksyNegatiivista() {
        c.convert(Integer.toString(-1));
    }
}
