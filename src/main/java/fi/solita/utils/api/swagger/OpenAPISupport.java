package fi.solita.utils.api.swagger;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalA.exists;
import static fi.solita.utils.functional.FunctionalA.find;
import static fi.solita.utils.functional.FunctionalA.subtract;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.filters.OpenApiMethodFilter;
import org.springdoc.core.fn.RouterOperation;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import fi.solita.utils.api.Documentation;
import fi.solita.utils.api.base.VersionBase;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Predicate;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
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
    
    protected static final DateTime SOME_DATETIME = LocalDate.parse("1982-01-22").toDateTime(LocalTime.parse("13:20:45")).withZone(DateTimeZone.UTC);
    
    private static final Iterable<SerializationFormat> VISIBLE_FORMATS = subtract(SerializationFormat.values(), /*not implemented yet:*/ newList(SerializationFormat.XML, SerializationFormat.GML, SerializationFormat.MVT, SerializationFormat.PDF));
    
    static {
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;
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
            openApi.info(openApi.getInfo().title(title)
                   .description(description)
                   .version(publishedVersion.getVersion()));
            OpenAPISupport.this.customize(openApi);
        }
    }
    
    /**
     * Customize OpenAPI definition
     */
    protected void customize(OpenAPI openApi) {
    }
    
    @SuppressWarnings("unchecked")
    protected static <T extends Enum<T>> void enumValue(ApplyZero<Schema<?>> schema, Apply<T,String> f, Class<T> clazz) {
        List<String> vals = newList(map(f, ClassUtils.getEnumType(clazz).get().getEnumConstants()));
        ((Schema<String>)schema.get()).setEnum(vals);
        schema.get().example(head(vals));
    }
    
    /**
     * Customize parameter defined by OperationCustomizer
     */
    protected Option<Parameter> customize(boolean ignoreRevision, Parameter parameter, java.lang.reflect.Parameter methodParameter) {
        Class<?> type = methodParameter.getType();
        if (Revision.class.isAssignableFrom(type)) {
            if (ignoreRevision) {
                return None();
            } else {
                parameter.schema(new IntegerSchema());
            }
        }
        
        if (DateTime.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_DateTime)
                     .schema(new StringSchema()._default("2020-01-01T00:00:00Z").format("datetime"));
        } else if (Interval.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_IntervalPeriod)
                     .schema(new StringSchema().format("interval"));
        } else if (Period.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_Period)
                     .schema(new StringSchema().format("iso8601"));
        } else if (LocalDate.class.isAssignableFrom(type)) {
            parameter.description(DESCRIPTION_LocalDate)
                     .schema(new StringSchema().format("date"))
                     .example("1982-01-22");
        } else if (Count.class.isAssignableFrom(type)) {
            parameter
                .example(1)
                .schema(new IntegerSchema()._default(1)._enum(newList(Collections.newArray(Number.class, Count.validValues))));
        } else if (StartIndex.class.isAssignableFrom(type)) {
            parameter
                .schema(new IntegerSchema().minimum(BigDecimal.ONE));
        } else if (Filters.class.isAssignableFrom(type)) {
            parameter
                .description(DESCRIPTION_Filters)
                .schema(new StringSchema().format("ecql"));
        } else if (parameter.getName().equals("propertyName")) {
            parameter
                .example("tunniste,voimassa")
                .schema(new ArraySchema().items(new StringSchema()))
                .explode(false);
        } else if (SRSName.class.isAssignableFrom(type)) {
            parameter
                .example(SRSName.EPSG3067.value)
                .schema(new StringSchema()._enum(newList(map(SRSName_.value, SRSName.validValues))));
        } else if (parameter.getName().equals("typeNames")) {
            parameter
                .description("Palautettavat alityypit aakkosjärjestyksessä. Oletuksena kaikki. / Subtypes to return, in alphabetic order. All subtypes by default.")
                .schema(new ArraySchema().items(new StringSchema()))
                .explode(false);
        } else if (parameter.getName().equals("versio")) {
            parameter
                .description("Objektin versionumero / Object version number");
        } else if (Collection.class.isAssignableFrom(type)) {
            parameter.explode(false);
        }
        
        for (String s: doc(Option.of(methodParameter.getName()), MemberUtil.memberTypeUnwrappingOptionAndEitherAndIterables(methodParameter.getParameterizedType()), methodParameter.getAnnotations(), None())) {
            if (!s.isEmpty()) {
                parameter.description(s);
            }
        }
        
        return Some(parameter);
    }

    /** Make this a spring-bean to enable. */
    public class ModelConverterBase implements ModelConverter {
        private final Map<Class<?>, Class<?>> directSubstitutions;

        public ModelConverterBase(Map<Class<?>,Class<?>> directSubstitutions) {
            this.directSubstitutions = directSubstitutions;
        }
        
        @Override
        public final Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
            Schema<?> ret = ClassUtils.resolveClass(type.getType()).flatMap(new Apply<Class<?>,Option<Schema<?>>>() {
                @Override
                public Option<Schema<?>> apply(Class<?> clazz) {
                    if (Option.class.isAssignableFrom(clazz)) {
                        return Some(context.resolve(new AnnotatedType(ClassUtils.getFirstTypeArgument(type.getType()).getOrElse(Object.class))));
                    } else if (Either.class.isAssignableFrom(clazz)) {
                        Type left = ((ParameterizedType) type).getActualTypeArguments()[0];
                        Type right = ((ParameterizedType) type).getActualTypeArguments()[1];
                        Schema<?> schemaLeft = context.resolve(new AnnotatedType(left));
                        Schema<?> schemaRight = context.resolve(new AnnotatedType(right));
                        return Some(new Schema<>().anyOf(newList(schemaLeft, schemaRight)));
                    }
                    
                    for (Class<?> substitute: find(clazz, directSubstitutions)) {
                        return Some(context.resolve(new AnnotatedType(substitute)));
                    }
                    for (Map.Entry<Class<?>, Class<?>> e: directSubstitutions.entrySet()) {
                        if (e.getKey().isAssignableFrom(clazz)) {
                            return Some(context.resolve(new AnnotatedType(e.getValue())));
                        }
                    }
                    return Option.<Schema<?>>None();
                }
            }).orElse(new ApplyZero<Schema<?>>() {
                @Override
                public Schema<?> get() {
                    return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
                }
            });

            if (ret != null) {
                addRequiredProperties(type, context, ret);
                
                // Need to ensure all modifications are made to a clone, since springdoc reuses the same schema instances.
                Schema<?>[] effective = new Schema<?>[] {ret};
                ApplyZero<Schema<?>> schemaProvider = Function.memoize(new ApplyZero<Schema<?>>() {
                    @Override
                    public Schema<?> get() {
                        if (effective[0].get$ref() != null) {
                            String ref = effective[0].get$ref();
                            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
                            Schema<?> definedSchema = context.getDefinedModels().get(schemaName);
                            if (definedSchema != null) {
                                effective[0] = definedSchema;
                            }
                        }
                        effective[0] = cloneSchema(effective[0]);
                        return effective[0];
                    }
                });
                customize(type, context, schemaProvider);
                ret = effective[0];
            }
            return ret;
        }
        
        /**
         * Customize schema defined by ModelConverter
         */
        protected void customize(AnnotatedType type, ModelConverterContext context, ApplyZero<Schema<?>> schema) {
            for (Class<?> clazz: Some(MemberUtil.memberTypeUnwrappingOption(type.getType()))) {
                if (clazz.equals(DateTime.class)) {
                    schema.get().description(DESCRIPTION_DateTime)
                          .example(RequestUtil.instant2string(SOME_DATETIME));
                } else if (clazz.equals(Character.class)) {
                    schema.get().description("character")
                          .example("c");
                } else if (clazz.equals(Interval.class)) {
                    schema.get().format("interval")
                          .description(DESCRIPTION_Interval)
                          .example(RequestUtil.interval2stringRestrictedToInfinity(new Interval(SOME_DATETIME, SOME_DATETIME.plusHours(1))));
                } else if (clazz.equals(LocalDate.class)) {
                    schema.get().description(DESCRIPTION_LocalDate)
                          .format("date")
                          .example("1982-01-22");
                } else if (clazz.equals(URI.class)) {
                    schema.get().format("uri")
                          .description("URI")
                          .example("https://www.liikennevirasto.fi");
                } else if (clazz.equals(LocalTime.class)) {
                    schema.get().format("localtime")
                          .description("Kellonaika / Time. HH:mm:ss")
                          .pattern("[0-9]{2,2}:[0-9]{2,2}:[0-9]{2,2}")
                          .example("13:20:45");
                } else if (clazz.equals(Duration.class)) {
                    schema.get().format("duration")
                          .description("Kesto / Duration. ISO8601")
                          .example("PT67S");
                } else if (clazz.equals(DateTimeZone.class)) {
                    schema.get().format("datetimezone")
                          .description("Aikavyöhykekoodi / Time zone code")
                          .example("Europe/Helsinki");
                }
                
                for (String s: doc(Option.of(type.getPropertyName()), type.getType(), Option.of(type.getCtxAnnotations()).getOrElse(new Annotation[0]), None())) {
                    if (!s.isEmpty()) {
                        schema.get().description(s);
                    }
                }
            }
        }
        
    }
    
    private static void addRequiredProperties(AnnotatedType type, ModelConverterContext context, Schema<?> schema) {
        if (schema == null) {
            return;
        }

        Schema<?> resolvedSchema = schema;
        if (schema.get$ref() != null && schema.getProperties() == null) {
            String ref = schema.get$ref();
            // $ref is typically "#/components/schemas/ClassName"
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            resolvedSchema = context.getDefinedModels().get(schemaName);
        }

        if (resolvedSchema == null || resolvedSchema.getProperties() == null || resolvedSchema.getProperties().isEmpty()) {
            return;
        }

        JavaType javaType = Json.mapper().constructType(type.getType());
        BeanDescription beanDescription = Json.mapper().getSerializationConfig().introspect(javaType);

        for (BeanPropertyDefinition property: beanDescription.findProperties()) {
            AnnotatedMember member = property.getPrimaryMember();
            if (member != null && !Option.class.isAssignableFrom(member.getRawType()) && resolvedSchema.getProperties().containsKey(property.getName()) &&
                    (resolvedSchema.getRequired() == null || !resolvedSchema.getRequired().contains(property.getName()))) {
                resolvedSchema.addRequiredItem(property.getName());
            }
        }
    }
    
    protected final class OperationCustomizerBase implements OperationCustomizer {
        private final boolean ignoreRevision;
        private final boolean includeFormatParameter;

        private OperationCustomizerBase(boolean ignoreRevision, boolean includeFormatParameter) {
            this.ignoreRevision = ignoreRevision;
            this.includeFormatParameter = includeFormatParameter;
        }

        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            for (String d: doc(Some(handlerMethod.getMethod().getDeclaringClass().getName()), handlerMethod.getMethod().getDeclaringClass(), handlerMethod.getMethod().getDeclaringClass().getAnnotations(), None())) {
                operation.setTags(newList(d));
            }
            
            if (includeFormatParameter) {
                if (exists(new Predicate<String>() {
                    @Override
                    public boolean accept(String candidate) {
                        return candidate.contains("{format}");
                    }
                }, handlerMethod.getMethodAnnotation(RequestMapping.class).value())) {
                    StringSchema schema = new StringSchema();
                    schema.setEnum(newList(map(OpenAPISupport_.enumName.andThen(OpenAPISupport_.toLowerCase), VISIBLE_FORMATS)));
                    schema.setDefault("json");
                    operation.getParameters().add(0, new Parameter().name("format")
                                                                    .description("Vastauksen muoto / Response format")
                                                                    .in("path")
                                                                    .schema(schema));
                }
            }
            
            operation.setParameters(newList(flatMap(new Apply<Parameter,Option<Parameter>>() {
                @Override
                public Option<Parameter> apply(Parameter parameter) {
                    if (parameter.getName().equals("format")) {
                        return Some(parameter);
                    } else {
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
                        return OpenAPISupport.this.customize(ignoreRevision, parameter, methodParameter);
                    }
                }
            }, operation.getParameters())));
            
            return operation;
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Option<String> doc(Option<String> name, Type genericType, Annotation[] annotations, Option<Class<?>> declaringClass) {
        for (Documentation doc: (Option<Documentation>)(Object)find(OpenAPISupport_.equalsDocumentation, annotations)) {
            return Some(doc.name_en() + ": " + doc.description() + " / " + doc.description_en());
        }
        for (Documentation doc: (Option<Documentation>)(Object)find(OpenAPISupport_.equalsDocumentation, ClassUtils.resolveClass(genericType).get().getAnnotations())) {
            return Some(doc.name_en() + ": " + doc.description() + " / " + doc.description_en());
        }
        return None();
    }

    public GroupedOpenApi createGroupedOpenApi(VersionBase<?> publishedVersion, final boolean ignoreRevision, boolean includeFormatParameter, String title, String description) {
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
                    
                    return routerOperation;
                }
            })
            .addOperationCustomizer(new OperationCustomizerBase(ignoreRevision, includeFormatParameter))
            .build();
    }
    
    /**
     * Need to clone schemas if we modify them and use for another type, since they are shared instances.
     * Uses reflection to copy all fields (including @JsonIgnore and transient ones) while preserving the concrete subtype.
     */
    private static Schema<?> cloneSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        try {
            Schema<?> clone = (Schema<?>) schema.getClass().getDeclaredConstructor().newInstance();
            for (Class<?> c = schema.getClass(); c != null && !c.equals(Object.class); c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    field.set(clone, field.get(schema));
                }
            }
            return clone;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone schema", e);
        }
    }
    
    static boolean equalsDocumentation(Annotation a) {
        return a.annotationType().equals(Documentation.class);
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
