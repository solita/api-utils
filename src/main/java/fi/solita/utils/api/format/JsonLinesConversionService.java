package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class JsonLinesConversionService {

    public final JsonConversionService json;

    public JsonLinesConversionService(JsonConversionService json) {
        this.json = json;
    }
    
    private static final byte[] NEWLINE = "\n".getBytes(Charset.forName("UTF-8"));
    
    public byte[] serialize(Map<?,?> obj) {
        return serialize(obj.entrySet());
    }
    
    public byte[] serialize(Iterable<?> obj) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (Object e: obj) {
                os.write(json.serialize(e));
                os.write(NEWLINE);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> List<T> deserializeList(Class<T> targetClass, byte[] bytes) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8")));
            String line;
            List<T> ret = newMutableList();
            while ((line = reader.readLine()) != null) {
                ret.add(json.deserialize(targetClass, line));
            } 
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> List<T> deserializeList(Class<T> targetClass, String str) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(str));
            String line;
            List<T> ret = newMutableList();
            while ((line = reader.readLine()) != null) {
                ret.add(json.deserialize(targetClass, line));
            } 
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
