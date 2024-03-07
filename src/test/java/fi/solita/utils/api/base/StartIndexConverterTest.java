package fi.solita.utils.api.base;

import org.junit.Test;

import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.functional.Apply;

public class StartIndexConverterTest {

    @SuppressWarnings("unchecked")
    private Apply<String,StartIndex> c = (Apply<String, StartIndex>) new HttpSerializers(new Serializers()).converters().get(StartIndex.class);
    
    @Test
    public void hyvaksyyValidinArvon() {
        c.apply(Integer.toString(10));
    }
    
    @Test(expected = HttpSerializers.InvalidStartIndexException.class)
    public void eiHyvaksyNollaa() {
        c.apply(Integer.toString(0));
    }
    
    @Test(expected = HttpSerializers.InvalidStartIndexException.class)
    public void eiHyvaksyNegatiivista() {
        c.apply(Integer.toString(-1));
    }
}
