package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.max;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.repeat;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Functional.zip;
import static fi.solita.utils.functional.FunctionalA.subtract;
import static fi.solita.utils.functional.FunctionalS.range;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Function2;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.functional.Tuple2;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class ControllerTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(ControllerTestUtils.class);
    
    protected static Apply<Object,Option<String>> exceptionalEnumSerialization;
    
    /**
     * "plain" tarkoittaa päätteetöntä, eli "ei valittua formaattia, pelkkä URL-polku"
     */
    public static class PlainSerialization { private PlainSerialization() {} }
    public static final Either<SerializationFormat[],PlainSerialization> PLAIN_SERIALIZATION = Either.right(new PlainSerialization());
    
    // toistaiseksi toteutetut.
    // TODO: Miten PNG:t voisi testata, kun toteutus joutuu hakemaan jostakin urlista geojson-sisällön?
    public static final Either<SerializationFormat[],PlainSerialization> ALL_FORMATS = Either.left(newArray(SerializationFormat.class, subtract(SerializationFormat.values(), newList(SerializationFormat.XML, SerializationFormat.GML, SerializationFormat.PNG))));
    public static final Either<SerializationFormat[],PlainSerialization> ONLY_JSON = Either.left(new SerializationFormat[] { SerializationFormat.JSON });
    
    public static class Param {
        protected final String name;
        protected final String[] values;
        
        public Param(String name, String... values) {
            this.name = name;
            this.values = values;
        }

        public Param(String name, Iterable<? extends MetaNamedMember<?, ?>> properties) {
            this.name = name;
            this.values = newArray(String.class, sort(map(ControllerTestUtils_.memberName, properties)));
        }
        
        /**
         * If required==false, then a variant without the parameter is included in test cases
         */
        public boolean isRequired() {
            return false;
        }
        
        public List<String[]> variants() {
            return Collections.<String[]>newList(values);
        }
    }
    
    public static String memberName(MetaNamedMember<?, ?> member) {
        return member.getName();
    }
    
    public static final Param time() {
        return new Param("time", SwaggerSupport.intervalNow()) {
            @Override
            public boolean isRequired() {
                return true;
            }
        };
    }
    
    public static final Param srsName() {
        return new Param("srsName") {
            @Override
            public List<String[]> variants() {
                return Collections.<String[]>newList(map(SRSName_.value.andThen(new Apply<String,String[]>() {
                    @Override
                    public String[] apply(String v) {
                        return new String[] {v};
                    }
                }), SRSName.validValues));
            }
        };
    }
    
    public static final Param presentation() {
        return new Param("presentation", "diagram") {
            @Override
            public boolean isRequired() {
                return false;
            }
        };
    }
    
    public static final Param bbox() {
        return new Param("bbox") {
            @Override
            public List<String[]> variants() {
                return newList(Some(new String[] {"368928,6815744,401696,6848512"}));
            }
        };
    }
    
    public static final Param distance() {
        return new Param("distance", "42");
    }
    
    public static final Param requiredFoobar() {
        return new Param("foobar", "baz") {
            @Override
            public boolean isRequired() {
                return true;
            }
        };
    }
    
    public static final Param count() {
        return new Param("count") {
            @Override
            public List<String[]> variants() {
                return Collections.<String[]>newList(new String[]{"1"});
            }
        };
    }
    
    /**
     * Always include all given properties
     */
    public static final Param propertyNameStatic(Iterable<? extends MetaNamedMember<?,?>> properties) {
        return new Param("propertyName", properties) {
            @Override
            public List<String[]> variants() {
                return Collections.<String[]>newList(values);
            }
            @Override
            public boolean isRequired() {
                return true;
            }
        };
    }
    
    public static final Param propertyName(Iterable<? extends MetaNamedMember<?,?>> properties) {
        return new Param("propertyName", properties) {
            @Override
            public List<String[]> variants() {
                return newList(concat(newList(
                    values,                                     // kaikki kerralla
                    new String[] {""}),                         // tyhjä arvo
                    map(ControllerTestUtils_.wrap, values)));   // yksi kerrallaan
            }
        };
    }
    
    static boolean isAssignableFrom(Class<?> clazz, Class<?> otherClass) {
        return clazz.isAssignableFrom(otherClass);
    }
    
    static Class<?> mapGet(Map<?,Class<?>> map, Object key) {
        return map.get(key);
    }
    
    public static final Param cql_filter(Iterable<? extends MetaNamedMember<?,?>> properties, final Function2<Map<CharSequence, Class<?>>,String,Option<String>> customTestiarvoPropertylle) {
        @SuppressWarnings("unchecked")
        final Map<CharSequence, Class<?>> types = newMap(map(MemberUtil_.memberName, MemberUtil_.actualTypeUnwrappingOptionAndEither(), (Iterable<? extends MetaNamedMember<Object,?>>)properties));

        // Collectioneille ja Map:lle ei saada arvottua sopivaa arvoa, joten jätetään ne pois
        Function1<Apply<?, ?>, Class<?>> getMemberType = MemberUtil_.memberName.andThen(ControllerTestUtils_.mapGet.ap(types));
        Predicate<Apply<?, ?>> notACollection = not(getMemberType.andThen(ControllerTestUtils_.isAssignableFrom.ap(Collection.class)));
        Predicate<Apply<?, ?>> notAMap = not(getMemberType.andThen(ControllerTestUtils_.isAssignableFrom.ap(Map.class)));
        properties = filter(notACollection.and(notAMap), properties);
        
        return new Param("cql_filter", properties) {
            @Override
            public List<String[]> variants() {
                return newList(
                    map(ControllerTestUtils_.wrap, map(ControllerTestUtils_.testiarvoPropertylle.ap(customTestiarvoPropertylle.ap(types), types), values)));   // yksi kerrallaan
            }
        };
    }
    
    // tämä on hieman härskiä, mutta jos se riittää testeihin niin jees.
    static String testiarvoPropertylle(Apply<String,Option<String>> customTestiarvoPropertylle, Map<CharSequence, Class<?>> types, String propertyName) {
        for (String res: customTestiarvoPropertylle.apply(propertyName)) {
            return res;
        }
        
        Class<?> clazz = types.get(propertyName);
        try {
            if (Boolean.class.isAssignableFrom(clazz)) {
                return propertyName + "=true";
            } else if (Number.class.isAssignableFrom(clazz)) {
                return propertyName + "=" + 42;
            } else if (Character.class.isAssignableFrom(clazz)) {
                return propertyName + "='a'";
            } else if (LocalDate.class.isAssignableFrom(clazz)) {
                return propertyName + "='" + LocalDate.now().toString() + "'";
            } else if (DateTime.class.isAssignableFrom(clazz)) {
                return propertyName + "='" + DateTime.now().toString(ISODateTimeFormat.dateTimeNoMillis()) + "'";
            } else {
                for (Class<?> e: ClassUtils.getEnumType(clazz)) {
                    String value = ((Enum<?>)e.getEnumConstants()[0]).name().toLowerCase();
                    if (exceptionalEnumSerialization != null) {
                        for (String s: exceptionalEnumSerialization.apply(e.getEnumConstants()[0])) {
                            value = s;
                        }
                    }
                    return propertyName + "='" + value + "'";
                }
                return propertyName + "='" + "foo" + "'";
            }
        } catch (Exception e) {
            throw new RuntimeException("Ei saatu testiarvoa propertylle '" + propertyName + "' tyyppiä " + clazz.getName(), e);
        }
    }
    
    static String[] wrap(String a) {
        return new String[]{a};
    }
    
    public static final Param typeNames(Iterable<String> values) {
        return new Param("typeNames", newArray(String.class, values)) {
            @Override
            public List<String[]> variants() {
                return newList(concat(newList(
                        values,                                     // kaikki kerralla
                        new String[] {""}),                         // tyhjä arvo
                        map(ControllerTestUtils_.wrap, values)));   // yksi kerrallaan
            }
        };
    }
    
    public void test_notime(String apiVersionPath, String path) {
        ResultMatcher expected = CompositeResultMatcher.of(
            status().is(307),
            redirectedUrl(apiVersionPath + path + ".json?foobar=baz&time=" + SwaggerSupport.intervalNow()));
        executeTest(apiVersionPath, expected, path, Collections.<String>emptyList(), new Param[] {requiredFoobar()}, ".json");
    }

    private final MockMvc mock;
    
    public ControllerTestUtils(MockMvc mock) {
        this.mock = mock;
    }
    
    protected abstract String basePath(String path);
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test200(String path, Param... params) throws RuntimeException {
        test200(path, emptyList(), params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test200(String path, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test200(path, emptyList(), formats, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test200(String path, List<?> pathVariables, Param... params) throws RuntimeException {
        test200(path, pathVariables, PLAIN_SERIALIZATION, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test200(String path, List<?> pathVariables, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test(status().is(200), path, pathVariables, formats, params);
    }
    

    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test302(String path, Param... params) throws RuntimeException {
        test302(path, emptyList(), params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test302(String path, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test302(path, emptyList(), formats, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test302(String path, List<?> pathVariables, Param... params) throws RuntimeException {
        test302(path, pathVariables, PLAIN_SERIALIZATION, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test302(String path, List<?> pathVariables, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test(status().is(302), path, pathVariables, formats, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test307(String path, Param... params) throws RuntimeException {
        test307(path, emptyList(), params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test307(String path, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test307(path, emptyList(), formats, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test307(String path, List<?> pathVariables, Param... params) throws RuntimeException {
        test307(path, pathVariables, PLAIN_SERIALIZATION, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test307(String path, List<?> pathVariables, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        test(status().is(307), path, pathVariables, formats, params);
    }
    
    /**
     * Absolute path is relative to api version. Relative path is relative to revision number which comes after api version.
     */
    public void test(ResultMatcher expectation, String path, List<?> pathVariables, Either<SerializationFormat[],PlainSerialization> formats, Param... params) throws RuntimeException {
        if (formats.isRight()) {
            executeTest(basePath(path), expectation, path, pathVariables, params, "");
        } else {
            for (SerializationFormat format: formats.left.get()) {
                executeTest(basePath(path), expectation, path, pathVariables, params, "." + format.name().toLowerCase());
            }
        }
    }
    
    static <T> Option<T> some(T t) {
        return Some(t);
    }
    
    static List<Option<String[]>> withOptionality(Param param) {
        Iterable<Option<String[]>> variants = map(ControllerTestUtils_.<String[]>some(), param.variants());
        return param.isRequired() ? newList(variants) : newList(cons(Option.<String[]>None(), variants));
    }
    
   
    public void executeTest(String basePath, ResultMatcher expectation, String path, List<?> pathVariables, Param[] params, String extension) {
        boolean aTestExecuted = false;
        
        // tehdään niin monta requestia kuin maksimissaan on jollakin parametrilla variantteja
        List<Set<Pair<String,String[]>>> requests = newList();
        for (@SuppressWarnings("unused") long i: range(1l, max(map(ControllerTestUtils_.Param_.variants.andThen(Transformers.size), params)).getOrElse(1l))) {
            requests.add(Collections.<Pair<String,String[]>>newSortedSet(Compare.by_1));
        }
        
        // populoidaan requestit parametrivarianteilla loopaten
        for (Param param: params) {
            for (Tuple2<Set<Pair<String, String[]>>, Option<String[]>> pair: zip(requests, flatten(repeat(withOptionality(param))))) {
                for (String[] args: pair._2){
                    pair._1.add(Pair.of(param.name, args));
                }
            }
        }
        
        for (Set<Pair<String,String[]>> requestParams: requests) {
            // pitää rakentaa querystring itse koska spring jättää sen pois :(
            String url = basePath + path + extension + (requestParams.isEmpty() ? "" : "?" + mkString("&", map(ControllerTestUtils_.paramsToString, requestParams)));
            aTestExecuted = true;
            executeTest(url, expectation, pathVariables);
        }
        Assert.True(aTestExecuted);
    }
    
    private void executeTest(String url, ResultMatcher expectation, List<?> pathVariables) {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(url, newArray(Object.class, pathVariables));
       
        try {
            logger.info("Testing url: {} with path variables: {}", url, pathVariables);
            mock.perform(req)
                .andDo(new LogOnExpectationFailureResultHandler(expectation))
                .andExpect(expectation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class LogOnExpectationFailureResultHandler implements ResultHandler {
        private final ResultMatcher expectation;

        public LogOnExpectationFailureResultHandler(ResultMatcher expectation) {
            this.expectation = expectation;
        }
        
        @Override
        public void handle(MvcResult result) throws Exception {
            StringWriter stringWriter = new StringWriter();
            ResultHandler printingResultHandler = MockMvcResultHandlers.print(new PrintWriter(stringWriter));
            try {
                expectation.match(result);
                if (logger.isDebugEnabled()) {
                    printingResultHandler.handle(result);
                    logger.debug("MvcResult details:\n" + stringWriter);
                }
            } catch (Error e) {
                if (logger.isWarnEnabled()) {
                    printingResultHandler.handle(result);
                    logger.warn("MvcResult details:\n" + stringWriter);
                }
            }
        }
    }
    
    static String paramsToString(String key, String[] value) {
        return key + "=" + mkString(",", value);
    }
}
