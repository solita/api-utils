package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Functional.size;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

public class CountConversionService {

    public byte[] serialize(Map<?,?> obj) {
        return Integer.toString(obj.size()).getBytes(Charset.forName("UTF-8"));
    }
    
    public byte[] serialize(Collection<?> obj) {
        return Integer.toString(obj.size()).getBytes(Charset.forName("UTF-8"));
    }
    
    public byte[] serialize(Iterable<?> obj) {
        return Long.toString(size(obj)).getBytes(Charset.forName("UTF-8"));
    }
    
    public byte[] serialize(Object obj) {
        return Integer.toString(1).getBytes(Charset.forName("UTF-8"));
    }
}