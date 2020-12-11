package fi.solita.utils.api.base.http;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;

import fi.solita.utils.api.base.StringToCollectionConverter;

public final class HttpModule extends FormattingConversionService {

    @SuppressWarnings("unchecked")
    public HttpModule(Map<Class<?>, Converter<String,?>> converters) {
        addConverter(new StringToCollectionConverter(this));
        for (Map.Entry<Class<?>, Converter<String, ?>> e: converters.entrySet()) {
            addConverter(String.class, (Class<Object>)e.getKey(), e.getValue());
        }
    }
}
