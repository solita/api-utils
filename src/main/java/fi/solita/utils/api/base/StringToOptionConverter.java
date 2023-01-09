package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import fi.solita.utils.functional.Option;

public class StringToOptionConverter implements ConditionalGenericConverter {

    private final ConversionService conversionService;

    public StringToOptionConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Option.class));
    }

    /**
     * Aargh, Springiin on kiinteästi koodattu tuki Arraylle, Streameille ja Collectioneille,
     * joten muiden genericsien tuki pitää lisätä rumasti...
     */
    private TypeDescriptor target(TypeDescriptor targetType) {
        return new TypeDescriptor(targetType.getResolvableType().getGeneric(), null, targetType.getAnnotations());
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.getType().equals(Option.class)) {
            return this.conversionService.canConvert(sourceType, target(targetType));
        } else {
            return false;
        }
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return None();
        }
        return Some(this.conversionService.convert(source, sourceType, target(targetType)));
    }
}
