package fi.solita.utils.api.format;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

public class JsonConversionService {

    public final ObjectMapper om = new JsonObjectMapper();

    public JsonConversionService(Module... modules) {
        for ( Module module : modules ) {
            this.om.registerModule(module);
        }
    }
    
    public byte[] serialize(Object obj) {
        try {
            return om.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> T deserialize(Class<T> targetClass, byte[] bytes) {
        try {
            return om.readValue(bytes, targetClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> T deserialize(Class<T> targetClass, String str) {
        try {
            return om.readValue(str, targetClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> List<T> deserializeList(Class<T> targetClass, byte[] bytes) {
        try {
            CollectionType type = om.getTypeFactory().constructCollectionType(List.class, targetClass);
            return om.readValue(bytes, type);
        } catch ( Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> List<T> deserializeList(Class<T> targetClass, String str) {
        try {
            CollectionType type = om.getTypeFactory().constructCollectionType(List.class, targetClass);
            return om.readValue(str, type);
        } catch ( Exception e) {
            throw new RuntimeException(e);
        }
    }
}
