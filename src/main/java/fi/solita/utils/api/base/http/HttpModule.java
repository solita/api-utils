package fi.solita.utils.api.base.http;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;

import fi.solita.utils.api.base.StringToCollectionConverter;

public final class HttpModule extends FormattingConversionService {

    public HttpModule(Map<Class<?>,? extends Converter<?,?>> converters) {
        addConverter(new StringToCollectionConverter(this));
        for (Map.Entry<Class<?>, ? extends Converter<?, ?>> e: converters.entrySet()) {
            addConverter(String.class, e.getKey(), e.getValue());
        }
    }
}