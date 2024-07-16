package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Functional.find;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import fi.solita.utils.api.JsonDeserializeAsBean;
import fi.solita.utils.functional.Apply;

public class StringToBeanDeserializableEnumConverter implements ConditionalGenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Enum.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        boolean isEnum = targetType.getType().isEnum();
        boolean isDeserializable = targetType.getType().isAnnotationPresent(JsonDeserializeAsBean.class);
        return isEnum && isDeserializable;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        return find(equalTo(((String)source).toLowerCase()), map(new Apply<Object,String>() {
            @Override
            public String apply(Object t) {
                return ((Enum)t).name().toLowerCase();
            }
        }, targetType.getType().getEnumConstants())).get();
    }
}
