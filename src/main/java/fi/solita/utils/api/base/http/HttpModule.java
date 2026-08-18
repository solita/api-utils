package fi.solita.utils.api.base.http;

import java.util.Map;
import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;

import fi.solita.utils.api.base.StringToBeanDeserializableEnumConverter;
import fi.solita.utils.api.base.StringToCollectionConverter;
import fi.solita.utils.api.base.StringToOptionConverter;

public final class HttpModule extends FormattingConversionService {

    @SuppressWarnings("unchecked")
    public HttpModule(Map<Class<?>, Function<String,?>> converters) {
        addConverter(new StringToCollectionConverter(this));
        addConverter(new StringToOptionConverter(this));
        addConverter(new StringToBeanDeserializableEnumConverter());
        for (final Map.Entry<Class<?>, Function<String, ?>> e: converters.entrySet()) {
            addConverter(String.class, (Class<Object>)e.getKey(), new Converter<String,Object>() {
                @Override
                public Object convert(String source) {
                    return e.getValue().apply(source);
                }
            });
        }
    }
}
