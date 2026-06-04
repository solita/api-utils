package fi.solita.utils.api.swagger;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.init;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalA.find;
import static fi.solita.utils.functional.FunctionalA.subtract;
import static fi.solita.utils.functional.FunctionalC.tail;
import static fi.solita.utils.functional.FunctionalS.range;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.matches;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.SimpleType;

import fi.solita.utils.api.Documentation;
import fi.solita.utils.api.JsonDeserializeAsBean;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.VersionBase;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Tuple;
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
    
    public static final String DESCRIPTION_DateTime       = "- Ajanhetki\n- Instant\n- `yyyy-MM-dd'T'HH:mm:ss'Z'`";
    public static final String DESCRIPTION_Interval       = "- Aikaväli\n- Interval\n- [ISO8601](https://en.wikipedia.org/wiki/ISO_8601#Time_intervals) varian 1\n- `yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'`";
    public static final String DESCRIPTION_IntervalPeriod = "- Aikaväli ilmaistuna joko kahdella UTC-ajanhetkellä tai UTC-ajanhetkellä ja periodilla\n- Interval expressed either with two UTC instants or a UTC-instant and a period\n- [ISO8601](https://en.wikipedia.org/wiki/ISO_8601#Time_intervals) variants 1-3\n- `yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'` | `yyyy-MM-dd'T'HH:mm:ss'Z'/PyYmMwWdDThHmMsS` | `PyYmMwWdDThHmMsS/yyyy-MM-dd'T'HH:mm:ss'Z'`";
    public static final String DESCRIPTION_Period         = "- Aikaväli ilmaistuna suhteessa nykyhetkeen joko yhdellä (negaatio tarkoittaa ajassa taaksepäin) tai kahdella periodilla\n- Interval relative to current time expressed either with one (negation indicates backwards in time) or two periods\n- [ISO8601](https://en.wikipedia.org/wiki/ISO_8601#Durations)\n- `PyYmMwWdDThHmMsS` | `-PyYmMwWdDThHmMsS` | `PyYmMwWdDThHmMsS/PyYmMwWdDThHmMsS`";
    public static final String DESCRIPTION_Duration       = "- Kesto\n- Duration\n- [ISO8601](https://en.wikipedia.org/wiki/ISO_8601#Durations)";
    public static final String DESCRIPTION_LocalDate      = "- Päivämäärä\n- Date\n- `yyyy-MM-dd`";
    public static final String DESCRIPTION_LocalTime      = "- Kellonaika\n- Time\n- `HH:mm:ss`";
    public static final String DESCRIPTION_TimeZone       = "- Aikavyöhykekoodi\n- Time zone code";
    public static final String DESCRIPTION_Filters        = "- [ECQL](https://docs.geoserver.org/latest/en/user/filter/ecql_reference.html#filter-ecql-reference)-alijoukko, useita suodattimia voi erottaa sanalla ' AND ' ja useita ehtoja sanalla ' OR '\n- [ECQL](https://docs.geoserver.org/latest/en/user/filter/ecql_reference.html#filter-ecql-reference)-subset, multiple filters can be separated with ' AND ' and multiple conditions with ' OR '\n- `" + mkString(", ", Filters.SUPPORTED_OPERATIONS) + "`";
    public static final String DESCRIPTION_Character      = "- Merkki\n- Character";
    public static final String DESCRIPTION_URI            = "- URI";
    public static final String DESCRIPTION_Format         = "- Vastauksen muoto\n- Response format";
    
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
            openApi.getComponents().addSecuritySchemes(RequestUtil.API_KEY, new SecurityScheme().type(SecurityScheme.Type.APIKEY).name(RequestUtil.API_KEY).in(SecurityScheme.In.HEADER))
                                   .addSecuritySchemes(RequestUtil.BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme(RequestUtil.BEARER).bearerFormat("JWT"));
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
     * Customize parameter defined by OperationCustomizer.
     * @return A new schema to be used to completely replace the original one.
     */
    protected Option<Parameter> customize(boolean ignoreRevision, Parameter parameter, java.lang.reflect.Parameter methodParameter) {
        Class<?> unwrappedType = MemberUtil.memberTypeUnwrappingOptionAndEitherAndIterables(methodParameter.getParameterizedType());
        if (Revision.class.isAssignableFrom(unwrappedType)) {
            if (ignoreRevision) {
                return None();
            } else {
                parameter.schema(new IntegerSchema());
            }
        }
        
        if (DateTime.class.isAssignableFrom(unwrappedType)) {
            parameter.description(DESCRIPTION_DateTime)
                     .schema(new StringSchema().format("datetime"));
        } else if (Interval.class.isAssignableFrom(unwrappedType)) {
            parameter.description(DESCRIPTION_IntervalPeriod)
                     .schema(new StringSchema().format("interval"));
        } else if (Period.class.isAssignableFrom(unwrappedType)) {
            parameter.description(DESCRIPTION_Period)
                     .schema(new StringSchema().format("iso8601"));
        } else if (LocalDate.class.isAssignableFrom(unwrappedType)) {
            parameter.description(DESCRIPTION_LocalDate)
                     .schema(new StringSchema().format("date"));
        } else if (Count.class.isAssignableFrom(unwrappedType)) {
            parameter
                .example(1) // intentionally, so that Swagger-UI uses 1 as the default
                .schema(new IntegerSchema()._enum(newList(Collections.newArray(Number.class, Count.validValues))));
        } else if (StartIndex.class.isAssignableFrom(unwrappedType)) {
            parameter
                .schema(new IntegerSchema().minimum(BigDecimal.ONE));
        } else if (Filters.class.isAssignableFrom(unwrappedType)) {
            parameter
                .description(DESCRIPTION_Filters)
                .schema(new StringSchema().format("ecql"));
        } else if (SRSName.class.isAssignableFrom(unwrappedType)) {
            parameter
                .schema(new StringSchema()._enum(newList(map(SRSName_.value, SRSName.validValues))));
        } else if (parameter.getName().equals("typeNames")) {
            parameter
                .description("- Palautettavat alityypit aakkosjärjestyksessä. Oletuksena kaikki\n- Subtypes to return, in alphabetic order. All subtypes by default.")
                .schema(new ArraySchema().items(new StringSchema()))
                .explode(false);
        } else if (parameter.getName().equals("versio")) {
            parameter
                .description("- Objektin versionumero\n- Object version number");
        } else if (Collection.class.isAssignableFrom(MemberUtil.memberTypeUnwrappingOptionAndEither(methodParameter.getParameterizedType()))) {
            parameter.explode(false);
        }
        
        Pair<Option<String>,Option<String>> d = doc(Option.of(methodParameter.getName()), unwrappedType, methodParameter.getAnnotations(), None());
        if (d.left().isDefined() || d.right().isDefined()) {
            parameter.description(mkString("\n", concat(d.left(), d.right().map(OpenAPISupport_.langsToList))));
        }
        
        return Some(parameter);
    }
    
    /** Make this a spring-bean to enable. */
    public class ModelConverterBase implements ModelConverter {
        private final Set<Class<?>> ACCEPT = newSet(
            Object.class,
            String.class,
            String[].class,
            Boolean.class,
            Boolean[].class,
            Byte.class,
            Byte[].class,
            Character.class,
            Character[].class,
            Short.class,
            Short[].class,
            Integer.class,
            Integer[].class,
            Long.class,
            Long[].class,
            Float.class,
            Float[].class,
            Double.class,
            Double[].class,
            Date.class,
            Either.class,
            Option.class
        );

        protected void checkType(Schema<?> ret, Type type) {
            Class<?> clazz = ClassUtils.typeClass(type);
            if (!Map.class.isAssignableFrom(clazz) &&
                !String.class.isAssignableFrom(clazz) &&
                !Number.class.isAssignableFrom(clazz) &&
                !Tuple.class.isAssignableFrom(clazz) &&
                !clazz.isPrimitive() &&
                !(clazz.isArray() && clazz.getComponentType().isPrimitive()) &&
                !ACCEPT.contains(clazz) &&
                !"array".equals(ret.getType()) &&
                !clazz.isAnnotationPresent(JsonSerializeAsBean.class) &&
                !clazz.isAnnotationPresent(JsonDeserializeAsBean.class)) {
                throw new RuntimeException(ret.getTitle() + "/" + ret.getDescription() +  ": Schema for type " + clazz + " was not customized. If you want to use default schema, annotate your type with fi.solita.utils.api.JsonSerializeAsBean. Or override fi.solita.utils.api.swagger.OpenAPISupport.ModelConverterBase.checkType to skip this check.");
            } 
        }
        
        @Override
        public final Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
            Option<Class<?>> clazz_ = ClassUtils.resolveClass(type.getType());
            Schema<?> ret = null;
            if (!clazz_.isDefined()) {
                ret = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
            } else {
                final Type[] effectiveType = new Type[] { type.getType() };
                Class<?> clazz = clazz_.get();
                
                if (Option.class.isAssignableFrom(clazz)) {
                    Type nested = ClassUtils.getFirstTypeArgument(type.getType()).getOrElse(Object.class);
                    ret = context.resolve(new AnnotatedType(nested).resolveAsRef(true));
                    effectiveType[0] = nested;
                } else if (Either.class.isAssignableFrom(clazz)) {
                    Type left = ClassUtils.getFirstTypeArgument(type.getType()).get();
                    Type right = ClassUtils.getSecondTypeArgument(type.getType()).get();
                    Schema<?> schemaLeft = context.resolve(new AnnotatedType(left).resolveAsRef(true));
                    Schema<?> schemaRight = context.resolve(new AnnotatedType(right).resolveAsRef(true));
                    ret = new Schema<Object>().oneOf(newList(schemaLeft, schemaRight));
                } else if (Tuple.class.isAssignableFrom(clazz)) {
                    ret = new ArraySchema().items(new Schema<Object>().oneOf(newList(map(new Apply<Integer, Schema<?>>() {
                        @Override
                        public Schema<?> apply(Integer i) {
                            return context.resolve(new AnnotatedType(((SimpleType)type.getType()).containedType(i)).resolveAsRef(true));
                        }
                    }, init(range(0, ((SimpleType)type.getType()).containedTypeCount()))))));
                }
                
                // Need to ensure all modifications are made to a clone, since springdoc reuses the same schema instances.
                Schema<?>[] modified = new Schema<?>[] {ret};
                ApplyZero<Schema<?>> schemaProvider = Function.memoize(new ApplyZero<Schema<?>>() {
                    @Override
                    public Schema<?> get() {
                        if (modified[0] == null) {
                            modified[0] = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
                        }
                        if (modified[0].get$ref() != null) {
                            Schema<?> definedSchema = getRef(context, modified[0]);
                            if (definedSchema != null) {
                                modified[0] = definedSchema;
                            }
                        }
                        modified[0] = cloneSchema(modified[0]);
                        return modified[0];
                    }
                });
                
                try {
                    for (Schema<?> replacement: customize(type, context, schemaProvider)) {
                        modified[0] = Assert.notNull(replacement);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to customize schema for type " + type.getType(), e);
                }
                if (modified[0] == null) {
                    // default to regular resolution from chain.
                    modified[0] = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
                    ret = modified[0];
                }
                
                if (modified[0] != null) {
                    try {
                        postCustomize(type, context, schemaProvider);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to post-customize schema for type " + type.getType(), e);
                    }
                }
                
                if (modified[0] == ret && ret != null) {
                    // not modified or replaced -> check if should have been
                    checkType(ret.get$ref() != null ? getRef(context, ret) : ret, effectiveType[0]);
                } else {
                    ret = modified[0];
                }
            }
            
            return ret;
        }
        
        /**
         * Customize schema defined by ModelConverter
         */
        protected Option<Schema<?>> customize(AnnotatedType type, ModelConverterContext context, ApplyZero<Schema<?>> schema) {
            for (Class<?> clazz: Some(MemberUtil.memberTypeUnwrappingOption(type.getType()))) {
                if (clazz.equals(DateTime.class)) {
                    schema.get().description(DESCRIPTION_DateTime)
                          .example(RequestUtil.instant2string(SOME_DATETIME));
                } else if (clazz.equals(Character.class)) {
                    schema.get().description(DESCRIPTION_Character)
                          .example("c");
                } else if (clazz.equals(Interval.class)) {
                    return Some(new StringSchema().format("interval")
                                                  .description(DESCRIPTION_Interval)
                                                  .example(RequestUtil.interval2stringRestrictedToInfinity(new Interval(SOME_DATETIME, SOME_DATETIME.plusHours(1)))));
                } else if (clazz.equals(LocalDate.class)) {
                    return Some(new StringSchema().description(DESCRIPTION_LocalDate)
                                                  .format("date")
                                                  .example("1982-01-22"));
                } else if (clazz.equals(UUID.class)) {
                    return Some(new StringSchema().format("uuid"));
                } else if (clazz.equals(URI.class)) {
                    schema.get().format("uri")
                          .description(DESCRIPTION_URI)
                          .example("https://www.liikennevirasto.fi");
                } else if (clazz.equals(LocalTime.class)) {
                    return Some(new StringSchema().format("localtime")
                                                  .description(DESCRIPTION_LocalTime)
                                                  .pattern("[0-9]{2,2}:[0-9]{2,2}:[0-9]{2,2}")
                                                  .example("13:20:45"));
                } else if (clazz.equals(Duration.class)) {
                    return Some(new StringSchema().format("duration")
                                                  .description(DESCRIPTION_Duration)
                                                  .example("PT67S"));
                } else if (clazz.equals(Period.class)) {
                    return Some(new StringSchema().format("iso8601")
                                                  .description(DESCRIPTION_Period)
                                                  .example("P2Y4M6W5DT12H35M30S"));
                } else if (clazz.equals(DateTimeZone.class)) {
                    return Some(new StringSchema().format("datetimezone")
                                                  .description(DESCRIPTION_TimeZone)
                                                  .example("Europe/Helsinki"));
                } else if (clazz.equals(PropertyName.class)) {
                    return Some(new StringSchema().pattern("-?[a-zA-Z0-9_][.a-zA-Z0-9_]*[.*]?"));
                }
            }
            return None();
        }
        
        protected void postCustomize(AnnotatedType type, ModelConverterContext context, ApplyZero<Schema<?>> schema) {
            Annotation[] annotations = Option.of(type.getCtxAnnotations()).getOrElse(new Annotation[0]);
            Pair<Option<String>, Option<String>> d = doc(Option.of(type.getPropertyName()), type.getType(), annotations, None());
            for (String s: d.right()) {
                schema.get().description(langsToList(s));
            }
            for (Class<?> clazz: Some(MemberUtil.memberTypeUnwrappingOption(type.getType()))) {
                for (String title: getSchemaTitle(clazz)) {
                    schema.get().title(title);
                }
            }
            addRequiredProperties(type, context, schema.get());
        }
    }
    
    protected Option<String> getSchemaTitle(Class<?> clazz) {
        for (Documentation doc: Option.of(clazz.getAnnotation(Documentation.class))) {
            if (!doc.name().isEmpty()) {
                return Some(doc.name());
            }
        }
        return None();
    }
    
    private static Schema<?> getRef(ModelConverterContext context, Schema<?> refSchema) {
        String ref = refSchema.get$ref();
        String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
        Schema<?> definedSchema = context.getDefinedModels().get(schemaName);
        return definedSchema;
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
            Pair<Option<String>, Option<String>> d = doc(Some(handlerMethod.getBeanType().getName()), handlerMethod.getBeanType(), handlerMethod.getBeanType().getAnnotations(), None());
            operation.setTags(newList(mkString(" - ", concat(d.left(), d.right()))));
            
            Pair<Option<String>, Option<String>> dd = doc(Some(handlerMethod.getMethod().getName()), handlerMethod.getMethod().getReturnType(), handlerMethod.getMethod().getAnnotations(), Some(handlerMethod.getBeanType()));
            for (String s: dd.left()) {
                operation.setSummary(s);
            }
            for (String s: dd.right()) {
                operation.setDescription(langsToList(s));
            }
            
            if (operation.getDeprecated() != null && operation.getDeprecated()
                                                  && !handlerMethod.getMethod().isAnnotationPresent(Deprecated.class)
                                                  && !handlerMethod.getBeanType().isAnnotationPresent(Deprecated.class)) {
                operation.setDeprecated(false); // Don't mark as deprecated if not explicitly annotated as such (Springdoc considers also inherited class annotations)
            }
            
            if (includeFormatParameter && !exists(OpenAPISupport_.parameterIsFormat, Option.of(operation.getParameters()).getOrElse(emptyList()))) {
                if (operation.getParameters() == null) {
                    operation.setParameters(newMutableList());
                }
                for (String value: find(new Predicate<String>() {
                    @Override
                    public boolean accept(String candidate) {
                        return candidate.contains("{format}") || candidate.contains("{format:");
                    }
                }, handlerMethod.getMethodAnnotation(RequestMapping.class).value())) {
                    Iterable<String> visibleFormats = map(OpenAPISupport_.enumName.andThen(OpenAPISupport_.toLowerCase), VISIBLE_FORMATS);
                    String pattern = value.replaceFirst(".*[{]format([^}]*)[}].*", "$1");
                    StringSchema schema = new StringSchema();
                    if (pattern.isEmpty()) {
                        schema.setEnum(newList(visibleFormats));
                    } else {
                        Pattern pat = Pattern.compile(tail(pattern)); // strip leading ':'
                        schema.setEnum(newList(pattern.isEmpty() ? visibleFormats
                                                                 : filter(matches(pat), map(OpenAPISupport_.enumName.andThen(OpenAPISupport_.toLowerCase), SerializationFormat.values()))));
                    }
                    operation.getParameters().add(0, new Parameter().name("format")
                                                                    .description(DESCRIPTION_Format)
                                                                    .in("path")
                                                                    .schema(schema));
                }
            }
            
            operation.setParameters(new ArrayList<Parameter>(newList(flatMap(new Apply<Parameter,Option<Parameter>>() {
                @Override
                public Option<Parameter> apply(Parameter parameter) {
                    if (parameter.getName().equals("format")) {
                        return Some(parameter);
                    } else {
                        String paramName = parameter.getName();
                        String paramIn = parameter.getIn();
                        for (java.lang.reflect.Parameter methodParameter: find(new Predicate<java.lang.reflect.Parameter>() {
                            @Override
                            public boolean accept(java.lang.reflect.Parameter candidate) {
                                return "query".equalsIgnoreCase(paramIn) && candidate.isAnnotationPresent(RequestParam.class)
                                     ? candidate.getAnnotation(RequestParam.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(RequestParam.class).name().equalsIgnoreCase(paramName)
                                     : "path".equalsIgnoreCase(paramIn) && candidate.isAnnotationPresent(PathVariable.class)
                                     ? candidate.getAnnotation(PathVariable.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(PathVariable.class).name().equalsIgnoreCase(paramName)
                                     : "path".equalsIgnoreCase(paramIn) && candidate.isAnnotationPresent(MatrixVariable.class)
                                     ? candidate.getAnnotation(MatrixVariable.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(MatrixVariable.class).name().equalsIgnoreCase(paramName)
                                     : "header".equalsIgnoreCase(paramIn) && candidate.isAnnotationPresent(RequestHeader.class)
                                     ? candidate.getAnnotation(RequestHeader.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(RequestHeader.class).name().equalsIgnoreCase(paramName)
                                     : "cookie".equalsIgnoreCase(paramIn) && candidate.isAnnotationPresent(CookieValue.class)
                                     ? candidate.getAnnotation(CookieValue.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(CookieValue.class).name().equalsIgnoreCase(paramName)
                                     : paramIn == null && candidate.isAnnotationPresent(RequestPart.class)
                                     ? candidate.getAnnotation(RequestPart.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(RequestPart.class).name().equalsIgnoreCase(paramName)
                                     : paramIn == null && candidate.isAnnotationPresent(RequestAttribute.class)
                                     ? candidate.getAnnotation(RequestAttribute.class).value().equalsIgnoreCase(paramName) || candidate.getAnnotation(RequestAttribute.class).name().equalsIgnoreCase(paramName)
                                     : candidate.getName().equalsIgnoreCase(paramName);
                            }
                        }, handlerMethod.getMethod().getParameters())) {
                            return OpenAPISupport.this.customize(ignoreRevision, parameter, methodParameter);
                        }
                        return Some(parameter);
                    }
                }
            }, operation.getParameters()))));
            
            return operation;
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Pair<Option<String>,Option<String>> doc(Option<String> name, Type genericType, Annotation[] annotations, Option<Class<?>> declaringClass) {
        for (Documentation doc: (Iterable<Documentation>)(Object)concat(find(OpenAPISupport_.equalsDocumentation, annotations),
                                                                        find(OpenAPISupport_.equalsDocumentation, ClassUtils.resolveClass(genericType).get().getAnnotations()))) {
            return Pair.of(doc.name().isEmpty() && doc.name_en().isEmpty()               ? None() : str2option(mkString(" / ", concat(str2option(doc.name()), str2option(doc.name_en())))),
                           doc.description().isEmpty() && doc.description_en().isEmpty() ? None() : str2option(mkString(" / ", concat(str2option(doc.description()), str2option(doc.description_en())))));
        }
        return Pair.of(None(), None());
    }

    public GroupedOpenApi.Builder createGroupedOpenApi(VersionBase<?> version, final boolean ignoreRevision, boolean includeFormatParameter, String title, String description) {
        return GroupedOpenApi.builder()
            .group(version.getVersion() + (ignoreRevision ? "" : ".extended"))
            .packagesToScan(version.getBasePackage())
            .pathsToMatch("/" + version.getVersion() + "/**")
            .addOpenApiCustomizer(new OpenApiCustomizerBase(version, title, description))
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
            .addOperationCustomizer(new OperationCustomizerBase(ignoreRevision, includeFormatParameter));
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
    
    static boolean parameterIsFormat(Parameter p) {
        return "format".equals(p.getName());
    }
    
    public static String langsToList(String description) {
        return description.contains(" / ") ? "- " + description.replace(" / ", "\n- ") : description;
    }
    
    static Option<String> str2option(String s) {
        return s.isEmpty() ? None() : Option.Some(s);
    }
}
