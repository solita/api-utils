package fi.solita.utils.api.base.http;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;

import fi.solita.utils.api.base.StringToCollectionConverter;
import fi.solita.utils.api.base.StringToOptionConverter;
import fi.solita.utils.functional.Apply;

public final class HttpModule extends FormattingConversionService {

    @SuppressWarnings("unchecked")
    public HttpModule(Map<Class<?>, Apply<String,?>> converters) {
        addConverter(new StringToCollectionConverter(this));
        addConverter(new StringToOptionConverter(this));
        for (Map.Entry<Class<?>, Apply<String, ?>> e: converters.entrySet()) {
            addConverter(String.class, (Class<Object>)e.getKey(), new Converter<String,Object>() {
                @Override
                public Object convert(String source) {
                    return e.getValue();
                }
            });
        }
    }
}
