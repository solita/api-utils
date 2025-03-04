package fi.solita.utils.api.swagger;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalA.subtract;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static springfox.documentation.schema.Collections.collectionElementType;
import static springfox.documentation.schema.Collections.containerType;
import static springfox.documentation.schema.Collections.isContainerType;
import static springfox.documentation.schema.Types.typeNameFor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import fi.solita.utils.api.Documentation;
import fi.solita.utils.api.base.VersionBase;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.swagger.SwaggerSupport_.CustomTypeParameterBuilder_;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import io.swagger.annotations.ApiParam;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.service.AllowableRangeValues;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.PathDecorator;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spi.service.contexts.DocumentationContextBuilder;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spi.service.contexts.PathContext;
import springfox.documentation.spi.service.contexts.RequestMappingContext;
import springfox.documentation.spring.web.WebMvcRequestHandler;
import springfox.documentation.spring.web.paths.DefaultPathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.ApiResourceController;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

public abstract class SwaggerSupport extends ApiResourceController {
    
    public static final SecurityConfiguration SECURITY_CONFIGURATION = new SecurityConfiguration(null, null, null, null, null, ApiKeyVehicle.HEADER, RequestUtil.API_KEY, ",");
    
    protected SwaggerResourcesProvider getSwaggerResources() {
        try {
            Field field = ApiResourceController.class.getDeclaredField("swaggerResources");
            field.setAccessible(true);
            return (SwaggerResourcesProvider) field.get(this);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    public SwaggerSupport(final String groupName, final boolean ignoreRevision) {
        super(new SwaggerResourcesProvider() {
            @Override
            public List<SwaggerResource> get() {
                SwaggerResource resource = new SwaggerResource();
                resource.setLocation("/../v2/api-docs?group=" + groupName + (ignoreRevision ? "" : ".extended"));
                resource.setSwaggerVersion("2.0");
                return newList(resource);
            }
        });
        
        // Seems that if custom mappings are added to Properties.TYPE_FACTORY, it allows specifying 
        // custom "format"s for string types.
        // Oh dear lord...
        try {
            Class<?> clazz = Class.forName("springfox.documentation.swagger2.mappers.Properties");
            Field field = clazz.getDeclaredField("TYPE_FACTORY");
            field.setAccessible(true);
            // must remove final specifier to set a new map to the field.
            Field modifiersField;
            try {
                modifiersField = Field.class.getDeclaredField("modifiers");
            } catch (NoSuchFieldException e) {
                // Java 17: https://stackoverflow.com/a/74727966
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                modifiersField = null;
                for (Field each : fields) {
                    if ("modifiers".equals(each.getName())) {
                        modifiersField = each;
                        break;
                    }
                }
            }
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            @SuppressWarnings("unchecked")
            Map<String, Function<String, ? extends Property>> map = (Map<String, Function<String, ? extends Property>>) field.get(null);
            Map<String, Function<String, ? extends Property>> newMap = new HashMap<>();
            for (final Map.Entry<String, String> format: additionalStringFormats().entrySet()) {
                newMap.put(format.getKey(), new Function<String, Property>() {
                    @Override
                    public Property apply(String input) {
                        return new StringProperty(format.getValue());
                    }
                });
            }
            newMap.putAll(map);
            field.set(null, newMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected Map<String,String> additionalStringFormats() {
        return newMap(Pair.of("interval","interval"),
                      Pair.of("uri","uri"),
                      Pair.of("localtime","time"),
                      Pair.of("duration","duration"),
                      Pair.of("datetimezone", "timezone"));
    }
    
    // This is horrible...
    public static class NonInheritingOperationDeprecatedReader implements OperationBuilderPlugin {
      @Override
      public void apply(OperationContext context) {
        WebMvcRequestHandler handler;
        try {
            Field field = OperationContext.class.getDeclaredField("requestContext");
            field.setAccessible(true);
            RequestMappingContext c = (RequestMappingContext) field.get(context);
            Field field2 = RequestMappingContext.class.getDeclaredField("handler");
            field2.setAccessible(true);
            handler = (WebMvcRequestHandler) field2.get(c);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        boolean annotationOnMethod = handler.getHandlerMethod().getMethod().isAnnotationPresent(Deprecated.class);
        boolean annotationOnController = handler.declaringClass().isAnnotationPresent(Deprecated.class);
          
        context.operationBuilder().deprecated(String.valueOf(annotationOnMethod || annotationOnController));
      }

      @Override
      public boolean supports(DocumentationType delimiter) {
        return true;
      }
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
    
    public static final String DESCRIPTION_DateTime = "Ajanhetki / Instant. yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Interval = "Aikaväli / Interval. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_IntervalPeriod = "Aikaväli ilmaistuna joko kahdella UTC-ajanhetkellä tai UTC-ajanhetkellä ja ISO8601-periodilla. / Interval expressed either with two UTC instants or a UTC-instant and a ISO8601 period. yyyy-MM-dd'T'HH:mm:ss'Z'/yyyy-MM-dd'T'HH:mm:ss'Z' or yyyy-MM-dd'T'HH:mm:ss'Z'/PyYmMwWdDThHmMsS or PyYmMwWdDThHmMsS/yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DESCRIPTION_Period = "Aikaväli ilmaistuna suhteessa nykyhetkeen joko yhdellä (negaatio tarkoittaa ajassa taaksepäin) tai kahdella ISO8601-periodilla. / Interval relative to current time expressed either with one (negation indicates backwards in time) or two ISO8601-periods. PyYmMwWdDThHmMsS or -PyYmMwWdDThHmMsS or PyYmMwWdDThHmMsS/PyYmMwWdDThHmMsS";
    public static final String DESCRIPTION_LocalDate = "Päivämäärä / Date. yyyy-MM-dd";
    public static final String DESCRIPTION_Filters = "ECQL-alijoukko, useita suodattimia voi erottaa sanalla ' AND ' / ECQL-subset, multiple filters can be separated with ' AND '. " + mkString(", ", Filters.SUPPORTED_OPERATIONS);
    
    public static abstract class DocumentingModelPropertyBuilder implements ModelPropertyBuilderPlugin {
        protected static <T extends Enum<T>> void enumValue(ModelPropertyBuilder builder, Apply<T,String> f, Class<T> clazz) {
            List<String> vals = newList(map(f, ClassUtils.getEnumType(clazz).get().getEnumConstants()));
            builder.allowableValues(new AllowableListValues(vals, "string"))
                   .example(head(vals));
        }
        
        protected Option<String> doc(AnnotatedElement ae) {
            for (Documentation doc: Option.of(ae.getAnnotation(Documentation.class))) {
                return Some(doc.name_en() + ": " + doc.description() + " / " + doc.description_en());
            }
            return None();
        }
        
        protected void apply(String name, Class<?> clazz, AnnotatedElement ae, ModelPropertyBuilder builder) {
            if (clazz.equals(DateTime.class)) {
                builder.description(DESCRIPTION_DateTime)
                       .example(RequestUtil.instant2string(DateTime.now(DateTimeZone.UTC)));
            } else if (clazz.equals(Character.class)) {
                builder.description("character")
                       .example("c");
            } else if (clazz.equals(Interval.class)) {
                builder.qualifiedType("interval")
                       .description(DESCRIPTION_Interval)
                       .example(RequestUtil.interval2stringRestrictedToInfinity(new Interval(DateTime.now(DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC).plusHours(1))));
            } else if (clazz.equals(LocalDate.class)) {
                builder.description(DESCRIPTION_LocalDate)
                       .example("1982-01-22");
            } else if (clazz.equals(Count.class)) {
                builder.description(doc(Count.class).getOrElse(""))
                        .example(1);
            } else if (clazz.equals(StartIndex.class)) {
                builder.description(doc(StartIndex.class).getOrElse(""))
                       .example(1);
            } else if (clazz.equals(Filters.class)) {
                builder.description(DESCRIPTION_Filters)
                       .example("tunniste<>1.2.246.578.2.3.4");
            } else if (clazz.equals(PropertyName.class)) {
                builder.description(doc(PropertyName.class).getOrElse(""))
                       .example("tunniste<>1.2.246.578.2.3.4");
            } else if (clazz.equals(SRSName.class)) {
                builder.example(SRSName.EPSG3067.value);
            } else if (clazz.equals(URI.class)) {
                builder.qualifiedType("uri")
                       .description("URI")
                       .example("https://www.liikennevirasto.fi");
            } else if (clazz.equals(LocalTime.class)) {
                builder.qualifiedType("localtime")
                       .description("Kellonaika / Time. HH:mm:ss")
                       .pattern("[0-9]{2,2}:[0-9]{2,2}:[0-9]{2,2}")
                       .example("13:20:45");
            } else if (clazz.equals(Duration.class)) {
                builder.qualifiedType("duration")
                       .description("Kesto sekunneissa ja millisekunneissa / Duration in seconds and milliseconds. ISO8601")
                       .example("PT72.345S");
            } else if (clazz.equals(DateTimeZone.class)) {
                builder.qualifiedType("datetimezone")
                       .description("Aikavyöhykekoodi / Time zone code")
                       .example("Europe/Helsinki");
            } else if (Either.class.isAssignableFrom(clazz)) {
                Type type = ae instanceof Field ? ((Field)ae).getGenericType() : ((Method)ae).getGenericReturnType();
                if (type instanceof ParameterizedType && ((ParameterizedType)type).getRawType().getClass().getName().equals(Either.class.getName())) {
                    // delegate to both parts inversely, so left overrides right if they set same stuff
                    apply(name, ClassUtils.typeClass(((ParameterizedType) type).getActualTypeArguments()[1]), ae, builder);
                    apply(name, ClassUtils.typeClass(((ParameterizedType) type).getActualTypeArguments()[0]), ae, builder);
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
                    Class<?> clazz = MemberUtil.memberTypeUnwrappingOption((AccessibleObject)ae);
                    
                    ModelPropertyBuilder builder = context.getBuilder();
                    if (Option.class.isAssignableFrom(clazz)) {
                        clazz = ClassUtils.typeClass(ae instanceof Field ? ((Field)ae).getGenericType() : ((Method)ae).getGenericReturnType());
                    }
                    apply(name, clazz, ae, builder);
                    
                    for (String s: doc(ae)) {
                        if (!s.isEmpty()) {
                            builder.description(s);
                        }
                    }
                }
            }
        }

        @Override
        public boolean supports(DocumentationType delimiter) {
            return SwaggerPluginSupport.pluginDoesApply(delimiter);
        }
    }
    
    public static Option<String> str2option(String s) {
        return s.isEmpty() ? Option.<String>None() : Option.Some(s);
    }
    
    /**
     * Rekisteröi kuvauksia jne globaalisti tunnetuille parametrityypeille
     */
    // TODO: Tämän sisältö oikeastaan riippuu API-versiosta, eli siis voi muuttua rajapinnan kehittyessä. Miten saisi versiokohtaiseksi?
    public static abstract class CustomTypeParameterBuilder implements ParameterBuilderPlugin {
        protected static <T extends Enum<T>> void enumValue(ParameterBuilder builder, Apply<T,String> f, Class<T> clazz) {
            List<String> vals = newList(map(f, ClassUtils.getEnumType(clazz).get().getEnumConstants()));
            builder.allowableValues(new AllowableListValues(vals, "string"));
        }
        
        protected static String pathVariableName(PathVariable pv) {
            return pv.value();
        }
        
        protected static String requestParamName(RequestParam rp) {
            return rp.value();
        }
        
        protected Option<String> doc(Class<?> clazz) {
            for (Documentation doc: Option.of(clazz.getAnnotation(Documentation.class))) {
                return Some(doc.name_en() + ": " + doc.description() + " / " + doc.description_en());
            }
            return None();
        }
        
        @Override
        public final void apply(ParameterContext parameterContext) {
            Class<?> type = parameterContext.resolvedMethodParameter().getParameterType().getErasedType();
            if (Option.class.equals(type)) {
                ResolvedType resolvedActual = head(parameterContext.resolvedMethodParameter().getParameterType().getTypeParameters());
                type = resolvedActual.getErasedType();
                String typeName = typeNameFor(resolvedActual.getErasedType());
                if (typeName == null) {
                    typeName = "string";
                }
                ModelReference itemModel = null;
                if (isContainerType(resolvedActual)) {
                  ResolvedType elementType = collectionElementType(resolvedActual);
                  String itemTypeName = typeNameFor(elementType.getErasedType());
                  if (itemTypeName == null) {
                      itemTypeName = typeNameFor(parameterContext.alternateFor(elementType).getErasedType());
                  }
                  typeName = containerType(resolvedActual);
                  itemModel = new ModelRef(itemTypeName);
                }
                parameterContext.parameterBuilder().required(false)
                                                   .type(resolvedActual)
                                                   .allowMultiple(isContainerType(resolvedActual))
                                                   .modelRef(new ModelRef(typeName, itemModel));
            }
            
            Option<String> pathVariableName = Option.of(parameterContext.resolvedMethodParameter().findAnnotation(PathVariable.class).orElse(null)).map(CustomTypeParameterBuilder_.pathVariableName);
            Option<String> requestParamName = Option.of(parameterContext.resolvedMethodParameter().findAnnotation(RequestParam.class).orElse(null)).map(CustomTypeParameterBuilder_.requestParamName);
            apply(parameterContext, type, pathVariableName, requestParamName);
            Optional<ApiParam> apiparam = parameterContext.resolvedMethodParameter().findAnnotation(ApiParam.class);
            if (apiparam.isPresent() && !apiparam.get().value().trim().isEmpty()) {
                String fromAnnotation = apiparam.get().value();
                String fromBuilder = parameterContext.parameterBuilder().build().getDescription();
                if (fromAnnotation.equals(fromBuilder)) {
                    parameterContext.parameterBuilder().description(fromAnnotation);
                } else {
                    parameterContext.parameterBuilder().description(fromAnnotation + " " + fromBuilder);
                }
            }
        }
        
        protected void apply(ParameterContext parameterContext, Class<?> type, Option<String> pathVariableName, Option<String> requestParamName) {
            if (DateTime.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(DESCRIPTION_DateTime);
            } else if (Interval.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(DESCRIPTION_IntervalPeriod);
            } else if (Period.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                .description(DESCRIPTION_Period);
            } else if (LocalDate.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(DESCRIPTION_LocalDate);
            } else if (Count.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(doc(Count.class).getOrElse(""))
                    .defaultValue("1")
                    .allowableValues(new AllowableListValues(newList(map(SwaggerSupport_.int2string, Count.validValues)), "int"));
            } else if (StartIndex.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(doc(StartIndex.class).getOrElse(""))
                    .allowableValues(new AllowableRangeValues("1", null));
            } else if (Filters.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .defaultValue("")
                    .description(DESCRIPTION_Filters);
            } else if (requestParamName.getOrElse("").equals("propertyName")) {
                parameterContext.parameterBuilder()
                    .collectionFormat("csv")
                    .description(doc(PropertyName.class).getOrElse(""));
            } else if (SRSName.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .description(doc(SRSName.class).getOrElse(""))
                    .allowableValues(new AllowableListValues(newList(map(SRSName_.value, SRSName.validValues)), "string"));
            } else if (requestParamName.getOrElse("").equals("typeNames")) {
                parameterContext.parameterBuilder()
                    .collectionFormat("csv")
                    .description("Palautettavat alityypit aakkosjärjestyksessä. Oletuksena kaikki. / Subtypes to return, in alphabetic order. All subtypes by default.");
            } else if (pathVariableName.getOrElse("").equals("versio")) {
                parameterContext.parameterBuilder()
                    .description("Objektin versionumero / Object version number");
            } else if (Collection.class.isAssignableFrom(type)) {
                parameterContext.parameterBuilder()
                    .collectionFormat("csv");
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
    
    public static void addResourceHandler(ResourceHandlerRegistry registry, String version, boolean ignoreRevision) {
        try {
            String path = version + (ignoreRevision ? "" : ".extended");
            registry.addResourceHandler("/" + path + "/swagger*.html")
                    .addResourceLocations("classpath:/META-INF/resources/swagger-ui.html");
            registry.addResourceHandler("/" + path + "/webjars/**")
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

    // To make https come first in the produced API description
    protected static <T> T protocolsKenttaJarjestykseen(Class<T> clazz, T obj) {
        Field protocols = null;
        try {
            protocols = clazz.getDeclaredField("protocols");
            protocols.setAccessible(true);
            protocols.set(obj, new LinkedHashSet<>((Set<String>)protocols.get(obj)));
            return obj;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param info  
     */
    public static Docket createDocket(final String contextPath, TypeResolver typeResolver, VersionBase publishedVersion, ApiInfo info, final boolean ignoreRevision, boolean includeFormatParameter) {
        Docket docket = new Docket(DocumentationType.SWAGGER_2) {
            @Override
            public DocumentationContext configure(DocumentationContextBuilder builder) {
                return super.configure(protocolsKenttaJarjestykseen(DocumentationContextBuilder.class, builder));
            }
        }
            .groupName(publishedVersion.getVersion() + (ignoreRevision ? "" : ".extended"))
            .select()
              .apis(RequestHandlerSelectors.basePackage(publishedVersion.getBasePackage()))
              .apis(new Predicate<RequestHandler>() {
                @Override
                public boolean test(RequestHandler rh) {
                    // only document publid methods
                    return Modifier.isPublic(rh.getHandlerMethod().getMethod().getModifiers());
                }
            }).build()
            .pathProvider(new DefaultPathProvider() {
                @Override
                public String getOperationPath(String operationPath) {
                    if (operationPath.startsWith(contextPath)) {
                        // Due to springfox bug: https://github.com/springfox/springfox/issues/3030
                        operationPath = operationPath.substring(contextPath.length());
                    }
                    String ret = super.getOperationPath(operationPath);
                    if (ignoreRevision) {
                        ret = ret.replaceAll("[{][^}]*[rR]evision[}]/", "");      // Poistetaan revision-pathparam koska se tulee implisiittisesti redirectistä.
                    }
                    return ret.replace("{?*****}", "")     // Poistetaan springin precedenssejä varten lisätyt tähtöset näkyvistä.
                              .replace("{*****}", "")
                              .replace("{asterisk}", "*"); // Keino lisätä Springissä polkuun tähtönen, niin että tulee swagger-kuvaukseenkin oikein.

                }

                @Override
                protected String getDocumentationPath() {
                    return contextPath + "/";
                }
            })
            .securitySchemes(newList(new ApiKey(RequestUtil.API_KEY, RequestUtil.API_KEY, ApiKeyVehicle.HEADER.getValue())))
            .useDefaultResponseMessages(false)
            .genericModelSubstitutes(Option.class)
            
            // Http-parametreissa käytetyt tyypit, jotka eivät välttämättä satu tulemaan JsonModulen kautta
            .directModelSubstitute(Count.class, int.class)
            .directModelSubstitute(StartIndex.class, int.class)
            .directModelSubstitute(Filters.class, String.class)      // pitää olla mukana, muuten parametri katoaa rajapintakuvauksesta näkyvistä...
            .directModelSubstitute(SRSName.class, String.class);      // pitää olla mukana, muuten parametri katoaa rajapintakuvauksesta näkyvistä...
            
        if (includeFormatParameter) {
            docket.globalOperationParameters(newList(new ParameterBuilder()
                  .name("format")
                  .defaultValue("json")
                  .description("Vastauksen muoto / Response format")
                  .modelRef(new ModelRef("String"))
                  .parameterType("path")
                  .required(true)
                  .allowableValues(new AllowableListValues(newList(map(SwaggerSupport_.enumName.andThen(SwaggerSupport_.toLowerCase), subtract(SerializationFormat.values(), /*not implemented yet:*/ newList(SerializationFormat.XML, SerializationFormat.GML, SerializationFormat.MVT)))), "String"))
                  .build()
            ));
        }
        if (ignoreRevision) {
            docket = docket.ignoredParameterTypes(Revision.class);
        } else {
            docket = docket.directModelSubstitute(Revision.class, long.class);
        }
        
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
            
            // Due to a Springfox bug: https://github.com/springfox/springfox/issues/3452
            for (Class<?> keyClass: cons(String.class, publishedVersion.jsonModule.keySerializers.keySet())) {
                docket.alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(Map.class, keyClass, dms.getKey()),
                        typeResolver.resolve(Map.class, keyClass, dms.getValue()), Ordered.LOWEST_PRECEDENCE));
                docket.alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(SortedMap.class, keyClass, dms.getKey()),
                        typeResolver.resolve(SortedMap.class, keyClass, dms.getValue()), Ordered.LOWEST_PRECEDENCE - 1));
            }
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
