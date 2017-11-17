package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.isEmpty;

import java.util.Map;

public class NotFoundException extends RuntimeException {
    public static <T,C extends Iterable<T>> C assertFound(C it) throws NotFoundException {
        if ( isEmpty(it) ) {
            throw new NotFoundException();
        }
        return it;
    }
    
    public static <K,V,C extends Map<K,V>> C assertFound(C map) throws NotFoundException {
        if ( map.isEmpty() ) {
            throw new NotFoundException();
        }
        return map;
    }
}
