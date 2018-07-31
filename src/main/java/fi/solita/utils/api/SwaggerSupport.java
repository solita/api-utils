package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalA.subtract;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import fi.solita.utils.api.SwaggerSupport_.CustomTypeParameterBuilder_;
import fi.solita.utils.api.base.VersionBase;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.PathDecorator;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spi.service.contexts.PathContext;
import springfox.documentation.spring.web.paths.AbstractPathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.ApiResourceController;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

public abstract class SwaggerSupport extends ApiResourceController {
    
    public static final String API_KEY = "Api-Key";
    
    public static final SecurityConfiguration SECURITY_CONFIGURATION = new SecurityConfiguration(null, null, null, null, null, ApiKeyVehicle.HEADER, API_KEY, ",");
    
    public SwaggerSupport(final String groupName) {
        super(new SwaggerResourcesProvider() {
            @Override
            public List<SwaggerResource> get() {
                SwaggerResource resource = new SwaggerResource();
                resource.setLocation("/../v2/api-docs?group=" + groupName);
                resource.setSwaggerVersion("2.0");
                return newList(resource);
            }
        });
    }
    
    // To fix incorrect stripping of regex parts
    public static class PathSanitizerFixer implements PathDecorator, Ordered {
        @Override
        public Function<String, String> decorator(PathContext context) {
            return new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return input.replaceAll("\\{([^}]+?):[^}]+?\\}", "{$1}");
                }
            };
        }
        
        @Override
        public boolean supports(DocumentationContext dc) {
            return SwaggerPluginSupport.pluginDoesApply(dc.getDocumentationType());
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
    
    /**
     * Set Option:s optional, others required.
     */
    public static class OptionModelPropertyBuilder implements ModelPropertyBuilderPlugin {
        @Override
        public void apply(ModelPropertyContext context) {
            Optional<AnnotatedElement> definition = context.getAnnotatedElement();
            if (definition.isPresent()) {
                AnnotatedElement ae = definition.get();
                if (ae instanceof Field) {
                    if (Option.class.isAssignableFrom(((Field) ae).getType())) {
                       context.getBuilder().required(false);
                       return;
                    }
               }
            }
            context.getBuilder().required(true);
        }

        @Override
        public boolean supports(DocumentationType delimiter) {
            return SwaggerPluginSupport.pluginDoesApply(delimiter);
        }
    }
    
    public static final String DESCRIPTION_DateTime = "Ajanhetki, ilmaistuna aikavälinä / Instant, expressed as an interval. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Interval = "Aikaväli / Interval. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Filters = "ECQL-subset: " + mkString(", ", Filters.SUPPORTED_OPERATIONS);
    public static final String DESCRIPTION_SRSName = "Vastauksen koordinaattien SRS / SRS for response coordinates";
    
    public static abstract class DocumentingModelPropertyBuilder implements ModelPropertyBuilderPlugin {
        protected static <T extends Enum<T>> void enumValue(ModelPropertyBuilder builder, Apply<T,String> f, Class<T> clazz) {
            List<String> vals = newList(map(f, ClassUtils.getEnumType(clazz).get().getEnumConstants()));
            builder.allowableValues(new AllowableListValues(vals, "string"))
                   .example(head(vals));
        }
        
        protected abstract Option<String> doc(AnnotatedElement ae);
        
        protected void apply(String name, Class<?> clazz, AnnotatedElement ae, ModelPropertyBuilder builder) {
            if (clazz.equals(DateTime.class)) {
                builder.description(DESCRIPTION_DateTime)
                       .example(now());
            } else if (clazz.equals(Interval.class)) {
                builder.description(DESCRIPTION_Interval)
                       .example(intervalNow());
            } else if (clazz.equals(Count.class)) {
                builder.example("1");
            } else if (clazz.equals(Filters.class)) {
                builder.description(DESCRIPTION_Filters)
                       .example("tunniste<>1.2.246.578.2.3.4");
            } else if (clazz.equals(SRSName.class)) {
                builder.description(DESCRIPTION_SRSName)
                       .example("WGS84");
            } else if (clazz.equals(URI.class)) {
                builder.description("URI")
                       .example("https://www.liikennevirasto.fi");
            } else if (clazz.equals(LocalDate.class)) {
                builder.description("Päivämäärä / Date. yyyy-MM-dd")
                       .example("2014-02-14");
            } else if (clazz.equals(LocalTime.class)) {
                builder.description("Kellonaika / Time. HH:mm:ss")
                       .example("13:20:45");
            } else if (clazz.equals(Duration.class)) {
                builder.description("Kesto sekunneissa ja millisekunneissa / Duration in seconds and milliseconds. ISO8601")
                       .example("PT72.345S");
            } else if (clazz.equals(DateTimeZone.class)) {
                builder.description("Aikavyöhykekoodi / Time zone code")
                       .example("Europe/Helsinki");
            } else {
                if (Option.class.isAssignableFrom(clazz)) {
                    Class<?> parameterClass = ClassUtils.typeClass(ae instanceof Field ? ((Field)ae).getGenericType() : ((Method)ae).getGenericReturnType());
                    apply(name, parameterClass, ae, builder);
                }
            }
        }
        
        @Override
        public final void apply(ModelPropertyContext context) {
            Optional<AnnotatedElement> definition = context.getAnnotatedElement();
            if (definition.isPresent()) {
                AnnotatedElement ae = definition.get();
                if (ae instanceof AccessibleObject) {
                    String name = ((Member)ae).getName();
                    Class<?> clazz = MemberUtil.memberTypeUnwrappingOptionAndEither((AccessibleObject)ae);
                    apply(name, clazz, ae, context.getBuilder());
                    
                    for (String s: doc(ae)) {
                        context.getBuilder().description(s);
                    }
                }
            }
        }

        @Override
        public boolean supports(DocumentationType delimiter) {
            return SwaggerPluginSupport.pluginDoesApply(delimiter);
        }
    }
    
    public static final String now() {
        return LocalDate.now(DateTimeZone.UTC).toDateTimeAtStartOfDay(DateTimeZone.UTC).toString(ISODateTimeFormat.dateTimeNoMillis());
    }
    
    public static final String intervalNow() {
        return now() + "/" + now();
    }
    
    
    /**
     * Rekisteröi kuvauksia jne globaalisti tunnetuille parametrityypeille
     */
    // TODO: Tämän sisältö oikeastaan riippuu API-versiosta, eli siis voi muuttua rajapinnan kehittyessä. Miten saisi versiokohtaiseksi?
    public static abstract class CustomTypeParameterBuilder implements ParameterBuilderPlugin {
        protected static String pathVariableName(PathVariable pv) {
            return pv.value();
        }
        
        protected static String requestParamName(RequestParam rp) {
            return rp.value();
        }
        
        protected abstract Option<String> doc(Class<?> clazz);
        
        @Override
        public final void apply(ParameterContext parameterContext) {
            Class<?> type = parameterContext.resolvedMethodParameter().getParameterType().getErasedType();
            Option<String> pathVariableName = Option.of(parameterContext.resolvedMethodParameter().findAnnotation(PathVariable.class).orNull()).map(CustomTypeParameterBuilder_.pathVariableName);
            Option<String> requestParamName = Option.of(parameterContext.resolvedMethodParameter().findAnnotation(RequestParam.class).orNull()).map(CustomTypeParameterBuilder_.requestParamName);
            apply(parameterContext, type, pathVariableName, requestParamName);
        }
        
        protected void apply(ParameterContext parameterContext, Class<?> type, Option<String> pathVariableName, Option<String> requestParamName) {
            if (DateTime.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .defaultValue(intervalNow())
                    .description(DESCRIPTION_DateTime);
            } else if (Interval.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .defaultValue(intervalNow())
                    .description(DESCRIPTION_Interval);
            } else if (Count.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(doc(Count.class).getOrElse(""))
                    .defaultValue("1")
                    .allowableValues(new AllowableListValues(newList(map(SwaggerSupport_.int2string, Count.validValues)), "int"));
            } else if (Filters.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .defaultValue("")
                    .description(DESCRIPTION_Filters);
            } else if (SRSName.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(DESCRIPTION_SRSName)
                    .allowableValues(new AllowableListValues(newList(map(SRSName_.value, SRSName.validValues)), "string"));
            } else if (requestParamName.getOrElse("").equals("propertyName")) {
                parameterContext.parameterBuilder()
                    .description("Palautettavat kentät aakkosjärjestyksessä. Oletuksena kaikki paitsi ei-pistemäiset geometriat / Attributes to return, in alphabetic order. All except non-point-like geoemtries by default.");
            } else if (requestParamName.getOrElse("").equals("typeNames")) {
                parameterContext.parameterBuilder()
                    .description("Palautettavat alityypit aakkosjärjestyksessä. Oletuksena kaikki. / Subtypes to return, in alphabetic order. All subtypes by default.");
            } else if (pathVariableName.getOrElse("").equals("versio")) {
                parameterContext.parameterBuilder()
                    .description("Objektin versionumero / Object version number");
            }
        }
        
        @Override
        public boolean supports(DocumentationType delimiter) {
            return true;
        }
    }
    
    static String int2string(Integer i) {
        return Integer.toString(i);
    }
    
    public static void addResourceHandler(ResourceHandlerRegistry registry, String version) {
        try {
            registry.addResourceHandler("/" + version + "/swagger*.html")
                    .addResourceLocations("classpath:/META-INF/resources/swagger-ui.html");
            registry.addResourceHandler("/" + version + "/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class ApiInfo {
        public final String title;
        public final String description;
        
        public ApiInfo(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
    
    public static Docket createDocket(final String contextPath, TypeResolver typeResolver, VersionBase publishedVersion, ApiInfo info) {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
            .groupName(publishedVersion.getVersion())
            .select()
              .apis(RequestHandlerSelectors.basePackage(publishedVersion.getBasePackage()))
              .apis(new Predicate<RequestHandler>() {
                @Override
                public boolean apply(RequestHandler rh) {
                    // only document publid methods
                    return Modifier.isPublic(rh.getHandlerMethod().getMethod().getModifiers());
                }
            }).build()
            .pathProvider(new AbstractPathProvider() {
                @Override
                public String getOperationPath(String operationPath) {
                    // poistetaan revision-pathparam koska se tulee implisiittisesti redirectistä.
                    return super.getOperationPath(operationPath)
                            .replace("{revision}/", "") // poistetaan revision-pathparam koska se tulee implisiittisesti redirectistä.
                            .replace("{*****}", ""); // poistetaan springin precedenssejä varten lisätyt tähtöset näkyvistä.

                }

                @Override
                protected String applicationPath() {
                    return contextPath;
                }

                @Override
                protected String getDocumentationPath() {
                    return contextPath;
                }
            })
            .securitySchemes(newList(new ApiKey(API_KEY, API_KEY, ApiKeyVehicle.HEADER.getValue())))
            .useDefaultResponseMessages(false)
            .genericModelSubstitutes(Option.class)
            
            // Http-parametreissa käytetyt tyypit, jotka eivät välttämättä satu tulemaan JsonModulen kautta
            .directModelSubstitute(Count.class, String.class)
            .directModelSubstitute(Filters.class, String.class) // pitää olla mukana, muuten parametri katoaa rajapintakuvauksesta näkyvistä...
            .directModelSubstitute(SRSName.class, String.class) // pitää olla mukana, muuten parametri katoaa rajapintakuvauksesta näkyvistä...
            
            .ignoredParameterTypes(Revision.class)
            .globalOperationParameters(newList(new ParameterBuilder()
                .name("format")
                .defaultValue("json")
                .description("Vastauksen muoto / Response format")
                .modelRef(new ModelRef("String"))
                .parameterType("path")
                .required(true)
                .allowableValues(new AllowableListValues(newList(map(SwaggerSupport_.enumName.andThen(SwaggerSupport_.toLowerCase), subtract(SerializationFormat.values(), /*not implemented yet:*/ newList(SerializationFormat.XML, SerializationFormat.GML)))), "String"))
                .build()
            ));
        
        for (Entry<Class<?>, Class<?>> dms: publishedVersion.jsonModule.rawTypes.entrySet()) {
            docket.directModelSubstitute(dms.getKey(), dms.getValue());
            
            // Pitää asettaa explisiittisesti collectionien sisällä olevat asiat,
            // sprinfox ei osaa mäpätä collectionien sisälle automaattisesti directModelSubstitutes perusteella.
            docket.alternateTypeRules(AlternateTypeRules.newRule(
                typeResolver.resolve(Option.class, dms.getKey()),
                typeResolver.resolve(dms.getValue()), Ordered.HIGHEST_PRECEDENCE));
            docket.alternateTypeRules(AlternateTypeRules.newRule(
                typeResolver.resolve(SortedSet.class, dms.getKey()),
                typeResolver.resolve(SortedSet.class, dms.getValue()), Ordered.HIGHEST_PRECEDENCE));
            docket.alternateTypeRules(AlternateTypeRules.newRule(
                typeResolver.resolve(List.class, dms.getKey()),
                typeResolver.resolve(List.class, dms.getValue()), Ordered.HIGHEST_PRECEDENCE));
            docket.alternateTypeRules(AlternateTypeRules.newRule(
                typeResolver.resolve(Set.class, dms.getKey()),
                typeResolver.resolve(Set.class, dms.getValue()), Ordered.HIGHEST_PRECEDENCE + 1));
            docket.alternateTypeRules(AlternateTypeRules.newRule(
                typeResolver.resolve(Collection.class, dms.getKey()),
                typeResolver.resolve(Collection.class, dms.getValue()), Ordered.HIGHEST_PRECEDENCE + 2));
        }
        
        return docket;
    }
    
    static String toLowerCase(String s) {
        return s.toLowerCase();
    }
    
    static String enumName(Enum<?> e) {
        return e.name();
    }
}