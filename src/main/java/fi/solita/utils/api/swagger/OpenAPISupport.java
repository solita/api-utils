package fi.solita.utils.api.swagger;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalA.find;
import static fi.solita.utils.functional.FunctionalA.subtract;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.filters.OpenApiMethodFilter;
import org.springdoc.core.fn.RouterOperation;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import fi.solita.utils.api.Documentation;
import fi.solita.utils.api.base.VersionBase;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;

public abstract class OpenAPISupport {
    
    public static final String DESCRIPTION_DateTime = "Ajanhetki / Instant. yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Interval = "Aikaväli / Interval. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_IntervalPeriod = "Aikaväli ilmaistuna joko kahdella UTC-ajanhetkellä tai UTC-ajanhetkellä ja ISO8601-periodilla. / Interval expressed either with two UTC instants or a UTC-instant and a ISO8601 period. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z' or yyyy-MM-dd'T'HH:mm:ss'Z'/PyYmMwWdDThHmMsS or PyYmMwWdDThHmMsS/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Period = "Aikaväli ilmaistuna suhteessa nykyhetkeen joko yhdellä (negaatio tarkoittaa ajassa taaksepäin) tai kahdella ISO8601-periodilla. / Interval relative to current time expressed either with one (negation indicates backwards in time) or two ISO8601-periods. PyYmMwWdDThHmMsS or -PyYmMwWdDThHmMsS or PyYmMwWdDThHmMsS/PyYmMwWdDThHmMsS";
    public static final String DESCRIPTION_LocalDate = "Päivämäärä / Date. yyyy-MM-dd";
    public static final String DESCRIPTION_Filters = "ECQL-alijoukko, useita suodattimia voi erottaa sanalla ' AND ' / ECQL-subset, multiple filters can be separated with ' AND '. " + mkString(", ", Filters.SUPPORTED_OPERATIONS);
    
    static {
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;
        
        SpringDocUtils.getConfig().replaceWithClass(Revision.class, long.class);
        SpringDocUtils.getConfig().replaceWithClass(Count.class, int.class);
        SpringDocUtils.getConfig().replaceWithClass(StartIndex.class, int.class);
        SpringDocUtils.getConfig().replaceWithClass(Filters.class, String.class);
        SpringDocUtils.getConfig().replaceWithClass(SRSName.class, String.class);
    }
    
    private final class OpenApiCustomizerBase implements OpenApiCustomizer {
        private final VersionBase<?> publishedVersion;
        private String title;
        private String description;

        private OpenApiCustomizerBase(VersionBase<?> publishedVersion, String title, String description) {
            this.publishedVersion = publishedVersion;
            this.title = title;
            this.description = description;
        }
        
        @Override
        public void customise(OpenAPI openApi) {
            openApi.getComponents().addSecuritySchemes("Api-Key", new SecurityScheme().type(SecurityScheme.Type.APIKEY).scheme("Api-Key").in(SecurityScheme.In.HEADER));
            /*for (Map.Entry<Class<?>, Class<?>> dms: publishedVersion.jsonModule.rawTypes.entrySet()) {
                openApi.getComponents().getSchemas().put(dms.getKey().getName(), ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(dms.getKey())).schema);
            }*/
            openApi.info(openApi.getInfo().title(title)
                   .description(description)
                   .version(publishedVersion.getVersion()));
        }
    }
    
    protected void customise(OpenAPI openApi) {
    }
    
    protected Option<String> doc(AnnotatedElement ae) {
        for (Documentation doc: Option.of(ae.getAnnotation(Documentation.class))) {
            return Some(doc.name_en() + ": " + doc.description() + " / " + doc.description_en());
        }
        return None();
    }
    
    protected Option<Parameter> handleParameter(Parameter parameter, java.lang.reflect.Parameter methodParameter) {
        Class<?> type = methodParameter.getType();
        if (Revision.class.isAssignableFrom(type)) {
            return None();
        }
        if (DateTime.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_DateTime);
        } else if (Interval.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_IntervalPeriod);
        } else if (Period.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_Period);
        } else if (LocalDate.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_LocalDate);
        } else if (Count.class.isAssignableFrom(type)) {
            parameter
                .description(doc(Count.class).getOrElse(""))
                .schema(parameter.getSchema()._default("1")._enum(newList(map(OpenAPISupport_.int2string, Count.validValues))));
        } else if (StartIndex.class.isAssignableFrom(type)) {
            parameter
                .description(doc(StartIndex.class).getOrElse(""))
                .schema(parameter.getSchema().minimum(BigDecimal.ONE));
        } else if (Filters.class.isAssignableFrom(type)) {
            parameter
                .description(DESCRIPTION_Filters)
                .schema(parameter.getSchema()._default(""));
        } else if (parameter.getName().equals("propertyName")) {
            parameter
                .description(doc(PropertyName.class).getOrElse(""))
                .explode(false);
        } else if (SRSName.class.isAssignableFrom(type)) {
            parameter
                .description(doc(SRSName.class).getOrElse(""))
                .schema(parameter.getSchema()._enum(newList(map(SRSName_.value, SRSName.validValues))));
        } else if (parameter.getName().equals("typeNames")) {
            parameter
                .description("Palautettavat alityypit aakkosjärjestyksessä. Oletuksena kaikki. / Subtypes to return, in alphabetic order. All subtypes by default.")
                .explode(false);
        } else if (parameter.getName().equals("versio")) {
            parameter
                .description("Objektin versionumero / Object version number");
        } else if (Collection.class.isAssignableFrom(type)) {
            parameter.explode(false);
        }
        return Some(parameter);
    }

    protected final class OperationCustomizerBase implements OperationCustomizer {
        private final boolean ignoreRevision;

        private OperationCustomizerBase(boolean ignoreRevision) {
            this.ignoreRevision = ignoreRevision;
        }

        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            if (ignoreRevision) {
                operation.setParameters(newList(flatMap(new Apply<Parameter,Option<Parameter>>() {
                    @Override
                    public Option<Parameter> apply(Parameter parameter) {
                        java.lang.reflect.Parameter methodParameter = find(new Predicate<java.lang.reflect.Parameter>() {
                            @Override
                            public boolean accept(java.lang.reflect.Parameter candidate) {
                                return candidate.isAnnotationPresent(RequestParam.class)
                                    ? candidate.getAnnotation(RequestParam.class).value().equals(parameter.getName())
                                    : candidate.getName().equals(parameter.getName());
                            }
                        }, handlerMethod.getMethod().getParameters()).orElse(new ApplyZero<java.lang.reflect.Parameter>() {
                            @Override
                            public java.lang.reflect.Parameter get() {
                                throw new RuntimeException("Could not find method parameter for " + parameter.getName() + " in " + handlerMethod.getMethod());
                            }
                        });
                        return handleParameter(parameter, methodParameter);
                    }
                }, operation.getParameters())));
            }
            return operation;
        }
    }
    
    protected Map<String,String> additionalStringFormats() {
        return newMap(Pair.of("interval","interval"),
                      Pair.of("uri","uri"),
                      Pair.of("localtime","time"),
                      Pair.of("duration","duration"),
                      Pair.of("datetimezone", "timezone"));
    }

    public GroupedOpenApi createDocket(VersionBase<?> publishedVersion, final boolean ignoreRevision, boolean includeFormatParameter, String title, String description) {
        return GroupedOpenApi.builder()
            .group(publishedVersion.getVersion() + (ignoreRevision ? "" : ".extended"))
            .packagesToScan(publishedVersion.getBasePackage())
            .pathsToMatch("/" + publishedVersion.getVersion() + "/**")
            .addOpenApiCustomizer(new OpenApiCustomizerBase(publishedVersion, title, description))
            .addOpenApiMethodFilter(new OpenApiMethodFilter() {
                @Override
                public boolean isMethodToInclude(Method method) {
                    return Modifier.isPublic(method.getModifiers()); // only document publid methods
                }
            })
            .addRouterOperationCustomizer(new RouterOperationCustomizer() {
                @Override
                public RouterOperation customize(RouterOperation routerOperation, HandlerMethod handlerMethod) {
                    if (ignoreRevision) {
                        routerOperation.setPath(routerOperation.getPath().replaceAll("[{][^}]*[rR]evision[}]/", "")); // Poistetaan revision-pathparam koska se tulee implisiittisesti redirectistä.
                    }
                    routerOperation.setPath(routerOperation.getPath().replace("{?*****}", "")      // Poistetaan springin precedenssejä varten lisätyt tähtöset näkyvistä.
                                                                     .replace("{*****}", "")
                                                                     .replace("{asterisk}", "*")); // Keino lisätä Springissä polkuun tähtönen, niin että tulee swagger-kuvaukseenkin oikein.
                    
                    if (includeFormatParameter && (routerOperation.getPath().contains("{format}") || routerOperation.getPath().contains("{format:"))) {
                        StringSchema schema = new StringSchema();
                        schema.setEnum(newList(map(OpenAPISupport_.enumName.andThen(OpenAPISupport_.toLowerCase), subtract(SerializationFormat.values(), /*not implemented yet:*/ newList(SerializationFormat.XML, SerializationFormat.GML, SerializationFormat.MVT, SerializationFormat.PDF)))));
                        schema.setDefault("json");
                        /*routerOperation.getOperationModel().addParametersItem(new Parameter()
                            .name("format")
                            .description("Vastauksen muoto / Response format")
                            .schema(schema)
                            .in("path")
                            .required(true));*/
                    }
                    return routerOperation;
                }
            })
            .addOperationCustomizer(new OperationCustomizerBase(ignoreRevision))
            .build();
    }
    
    static String toLowerCase(String s) {
        return s.toLowerCase();
    }
    
    static String enumName(Enum<?> e) {
        return e.name();
    }
    
    static String int2string(Integer i) {
        return Integer.toString(i);
    }
}
