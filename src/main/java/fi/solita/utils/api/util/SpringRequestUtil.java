package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalA.flatten;
import static fi.solita.utils.functional.FunctionalC.tail;
import static fi.solita.utils.functional.FunctionalC.takeWhile;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.annotation.Annotation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fi.solita.utils.meta.MetaMethod;

public abstract class SpringRequestUtil {

    public static final String resolvePath(Class<?> latestVersion) {
        String[] paths = latestVersion.getAnnotation(RequestMapping.class).value();
        return takeWhile(not(equalTo('/')), tail(paths[0]));
    }

    static boolean isRequestParam(Annotation a) {
        return a.annotationType().equals(RequestParam.class);
    }

    static String paramValue(Annotation rp) {
        return ((RequestParam)rp).value().equals("") ? ((RequestParam)rp).name() : ((RequestParam)rp).value();
    }

    public static final String[] resolveQueryParams(MetaMethod<?, ?> requestMethod) {
        return newArray(String.class, map(SpringRequestUtil_.paramValue, filter(SpringRequestUtil_.isRequestParam, flatten(requestMethod.getMember().getParameterAnnotations()))));
    }

}
