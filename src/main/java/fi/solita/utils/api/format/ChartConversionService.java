package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newMutableMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.reduce;
import static fi.solita.utils.functional.Functional.repeat;
import static fi.solita.utils.functional.Functional.sequence;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.Functional.transpose;
import static fi.solita.utils.functional.Functional.zip;
import static fi.solita.utils.functional.Functional.zipWithIndex;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.FunctionalM.mapValue;
import static fi.solita.utils.functional.FunctionalM.with;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.isNull;
import static fi.solita.utils.functional.Predicates.not;
import static org.rendersnake.HtmlAttributesFactory.http_equiv;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.AccessibleObject;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.html.HttpServletCanvas;
import fi.solita.utils.api.html.UI;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil_;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Monoids;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.SemiGroups;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

public class ChartConversionService {
    
    public static class CannotChartByStructureException extends RuntimeException {
        public final String name;

        public CannotChartByStructureException(String name) {
            this.name = name;
        }
    }
    
    private static final MetaNamedMember<?,?> DUMMY_MEMBER = new MetaNamedMember<Object, Object>() {
        @Override
        public Object apply(Object t) {
            return t;
        }

        @Override
        public AccessibleObject getMember() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return "";
        }
    };
    
    private JsonConversionService json;

    public ChartConversionService(JsonConversionService json) {
        this.json = json;
    }
    
    public <T> byte[] serialize(Request request, String title, T obj, final Includes<T> members) {
        return serialize(request, title, newList(obj), members);
    }
    
    public <T> byte[] serialize(Request request, String title, T[] obj) {
        return serialize(request, title, newList(obj));
    }
    
    @SuppressWarnings("unchecked")
    public <T> byte[] serialize(Request request, String title, final Iterable<T> obj) {
        return serialize(request, title, newList(filter(not(isNull()), obj)), new Includes<T>(newList((MetaNamedMember<T,T>)DUMMY_MEMBER), emptyList(), emptyList(), true, emptyList()));
    }
    
    public <T> byte[] serialize(Request request, String title, final Collection<T> obj, final Includes<T> members) {
        return serialize(title, request, obj, members.includesFromColumnFiltering);
    }
    
    public <K,V> byte[] serialize(Request request, String title, final Map<K,? extends Iterable<V>> obj, Includes<V> members) {
        return serialize(title, request, flatten(obj.values()), members.includesFromColumnFiltering);
    }
    
    public <K,V> byte[] serializeSingle(Request request, String title, final Map<K,V> obj, Includes<V> members) {
        return serialize(title, request, newList(obj.values()), members.includesFromColumnFiltering);
    }
    
    private static final Pattern P1 = Pattern.compile("^\\s*\\{\\s*\"");
    private static final Pattern P2 = Pattern.compile("\"\\s*:\\s*0\\s*\\}\\s*$");
    private static final Pattern P3 = Pattern.compile("\\{\\}");
    
    @SuppressWarnings("unchecked")
    static final Iterable<Object> handleCollections(Object s) {
        return s instanceof Iterable
                   ? (Iterable<Object>) s :
                     newList(s);
    }
    
    @SuppressWarnings("unchecked")
    static final Iterable<Object> recursivelyFlatten(Object o) {
        if (o instanceof Iterable) {
            return flatMap(ChartConversionService_.recursivelyFlatten, (Iterable<Object>) o);
        }
        return newList(o);
    }
    
    final <T> Apply<Apply<T, Object>, Map<String, Long>> categoryValues(List<T> categoryObjects) {
        return new Apply<Apply<T,Object>,Map<String,Long>>() {
              @SuppressWarnings("unchecked")
              @Override
              public Map<String, Long> apply(Apply<T,Object> m) {
                 return mapValue((Transformer<List<Object>, Long>)(Object)Transformers.size, groupBy(ChartConversionService_.toKey.ap(ChartConversionService.this), flatMap(Function.of(m).andThen(ChartConversionService_.handleCollections), categoryObjects)));
              }};
    }
    
    protected Option<Object> toNumeric(Object value) {
        if (value instanceof Number) {
            return Some(value);
        } else {
            String serialized = new String(json.serialize(value), StandardCharsets.UTF_8);
            try {
                return Some(Double.parseDouble(serialized));
            } catch (NumberFormatException e) {
                return None();
            }
        }
    }
    
    protected static Class<?> resolveType(MetaNamedMember<?, ?> m) {
        return m == DUMMY_MEMBER ? Object.class : MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(m);
    }
    
    protected static Class<?> resolvePropertyType(MetaNamedMember<?, ?> m) {
        return m == DUMMY_MEMBER ? Object.class : MemberUtil.actualTypeUnwrappingOptionAndEither(m);
    }
    
    protected boolean isLinear(MetaNamedMember<?,?> m) {
        return DateTime.class.isAssignableFrom(resolveType(m)) ||
               Interval.class.isAssignableFrom(resolveType(m));
    }
    
    private static Range<DateTime> toRange(Interval i) {
        return i.getStart().equals(i.getEnd()) ? Range.closed(RequestUtil.limit(i.getStart()), RequestUtil.limit(i.getEnd()))
                                               : Range.closedOpen(RequestUtil.limit(i.getStart()), RequestUtil.limit(i.getEnd()));
    }
    
    private static Range<DateTime> toRange(DateTime i) {
        return Range.closed(RequestUtil.limit(i), RequestUtil.limit(i));
    }
    
    String jsonSerializeKey(Object key) {
        if (key == null || key == None()) {
            return "-";
        }
        String serialized = new String(json.serialize(newMap(Pair.of(key, 0))), StandardCharsets.UTF_8);
        String ret = P3.matcher(P2.matcher(P1.matcher(serialized).replaceAll("")).replaceAll("")).replaceAll("");
        return ret.trim().isEmpty() ? "-" : ret;
    }
    
    private <T> BiFunction<List<T>, List<T>, List<T>> coalesce() {
        return new BiFunction<List<T>, List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> first, List<T> second) {
                if (first == null || first.isEmpty()) {
                    return second;
                }
                if (second == null || second.isEmpty()) {
                    return first;
                }
                if (first.size() == 1 && second.size() == 1) {
                    return Arrays.asList(head(first), head(second));
                }
                ArrayList<T> ret = new ArrayList<T>(first.size() + second.size());
                ret.addAll(first);
                ret.addAll(second);
                return ret;
            }
        };
    }

    private <T> Apply<Pair<MetaNamedMember<T, Object>, Object>, Iterable<Pair<String, Object>>> mkDataRows(Map<Object, Object> values) {
        return new Apply<Pair<MetaNamedMember<T,Object>,Object>,Iterable<Pair<String,Object>>>() {
             @Override
             public Iterable<Pair<String,Object>> apply(Pair<MetaNamedMember<T, Object>,Object> p) {
                 Object value = p.right();
                 Option<Object> num = toNumeric(value);
                 return newList(Pair.of(MemberUtil.memberName(p.left()), toKey(value)),
                                Pair.of("_" + MemberUtil.memberName(p.left()), num.isDefined() ? num.get() : values.computeIfAbsent(value, new java.util.function.Function<Object,Object>() {
                                             @Override
                                             public Object apply(Object t) {
                                                 return values.size() + 1;
                                             }})));
             }
         };
    }
    
    String toKey(Object o) {
        if (o instanceof Iterable) {
            if (isEmpty((Iterable<?>) o)) {
                return "-";
            }
            return mkString(",", map(ChartConversionService_.jsonSerializeKey.ap(ChartConversionService.this), recursivelyFlatten(o)));
        }
        return jsonSerializeKey(o);
    }
    
    private <T> byte[] serialize(String title, Request request, Iterable<T> objs, final Iterable<? extends MetaNamedMember<T, ?>> members_) throws CannotChartByStructureException {
        @SuppressWarnings("unchecked")
        List<MetaNamedMember<T, Object>> members = (List<MetaNamedMember<T, Object>>) newList(members_);
        final boolean xIsInstant  = !members.isEmpty() && DateTime.class.isAssignableFrom(resolveType(head(members)));
        final boolean xIsInterval = !members.isEmpty() && Interval.class.isAssignableFrom(resolveType(head(members)));
        final boolean xIsTemporal = xIsInstant || xIsInterval;
        final boolean xIsLinear = !members.isEmpty() && isLinear(head(members));
        final boolean isStacked = members.size() == 2;
        final boolean isGrouped = members.size() == 1 || xIsInterval || !xIsLinear;
        List<Map<Object,Object>> data = newMutableList();
        List<String> yNames = emptyList();
        if (!isEmpty(members)) {
            for (MetaNamedMember<T, ?> m: members) {
                Class<?> finalType = resolveType(m);
                Class<?> propertyType = resolvePropertyType(m);
                if (finalType.isAnnotationPresent(JsonSerializeAsBean.class) ||
                    Map.class.isAssignableFrom(propertyType) ||
                    Collection.class.isAssignableFrom(propertyType) && Tuple.class.isAssignableFrom(finalType)) {
                    // pure structure, or Maps, or collections of Tuples don't make sense.
                    throw new CannotChartByStructureException(m.getName());
                }
            }
            
            Function1<T, Object> x = Function.of(head(members));
            
            Set<Object> xValues = newSet(map(x, objs));
    
            final boolean xIsListOfValuesFromASingleRow = size(objs) == 1 && head(members) != DUMMY_MEMBER && Iterable.class.isAssignableFrom(resolvePropertyType(head(members)));
            
            if (members.size() == 1) {
                if (xIsListOfValuesFromASingleRow) {
                    // a single object with a single member that is a collection -> use the target collection as objects
                    objs = (Iterable<T>) head(xValues);
                    x = (Function1<T, Object>) Function.id();
                    xValues = newSet(map(x, objs));
                }
                
                // single member -> always chart counts
                yNames = newList("count");
                if (xIsTemporal) {
                    RangeMap<DateTime, List<T>> m = TreeRangeMap.create();
                    BiFunction<List<T>, List<T>, List<T>> c = coalesce();
                    for (T o: objs) {
                        for (Object oo: recursivelyFlatten(x.apply(o))) { // use all values if a collection. Is this fine?
                            m.merge(xIsInstant ? toRange((DateTime)oo)
                                               : toRange((Interval)oo), newList(o), c);
                        }
                    }
    
                    for (Map.Entry<Range<DateTime>, List<T>> entry: m.asMapOfRanges().entrySet()) {
                        data.add(newMap(Pair.of("c", entry.getKey().lowerEndpoint().getMillis()),
                                        Pair.of("count", entry.getValue().size())));
                    }
                } else {
                    for (Object category: xValues) {
                        List<T> categoryObjects = newList(filter(x.andThen(equalTo(category)), objs));
                        data.add(newMap(Pair.of("c", category),
                                        Pair.of("count", Long.valueOf(categoryObjects.size()))));
                    }
                }
            } else {
                List<T> objs_ = newList(objs);
                yNames = newList(sort(flatMap(new Apply<MetaNamedMember<T,Object>,Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(MetaNamedMember<T, Object> m) {
                        return newSet(flatMap(new Apply<T,Iterable<String>>() {
                            @Override
                            public Iterable<String> apply(T t) {
                                Object val = m.apply(t);
                                return val instanceof Iterable
                                    ? map(ChartConversionService_.jsonSerializeKey.ap(ChartConversionService.this), recursivelyFlatten(val))
                                    : newList(jsonSerializeKey(val));
                            }
                        }, objs_));
                    }}, tail(members))));
    
                if (xIsInterval) {
                    // y has at least one member, x is interval -> chart counts
                    RangeMap<DateTime, List<T>> m = TreeRangeMap.create();
                    BiFunction<List<T>, List<T>, List<T>> c = coalesce();
                    for (T o: objs) {
                        for (Object oo: recursivelyFlatten(x.apply(o))) { // use all values if a collection. Is this fine?
                            m.merge(toRange((Interval)oo), newList(o), c);
                        }
                    }
                    
                    for (Map.Entry<Range<DateTime>, List<T>> entry: m.asMapOfRanges().entrySet()) {
                        data.add(with(SemiGroups.failUnequal(), "c", entry.getKey().lowerEndpoint().getMillis(),
                                     reduce(Monoids.mapCombine(SemiGroups.longSum),
                                         map(categoryValues(entry.getValue()), tail(members)))));
                    }
                } else if (xIsLinear) {
                    // y has at least one member, x is linear -> chart value
                    yNames = newList(map(MemberUtil_.memberName, tail(members)));
                    
                    Map<Object,Object> values = newMutableMap();
                    @SuppressWarnings("unchecked")
                    Function1<T,Comparable<T>> xx = (Function1<T,Comparable<T>>)(Object)x;
                    
                    if (xIsListOfValuesFromASingleRow) {
                        Iterable<Iterable<Object>> allRows = map(ChartConversionService_.handleCollections, sequence(Assert.singleton(objs), members));
                        for (Iterable<Object> category: transpose(allRows)) {
                            Iterable<Object> categoryObjects = tail(category);
                            data.add(with(SemiGroups.failUnequal(), "c", toNumeric(head(category)),
                                    newMap(SemiGroups.failUnequal(),
                                         flatMap(mkDataRows(values), zip(tail(members), categoryObjects)))));
                        }
                    } else {
                        for (T t: newList(sort(Compare.by(xx), objs))) {
                            Iterable<Object> categoryObjects = sequence(t, tail(members));
                            data.add(with(SemiGroups.failUnequal(), "c", xIsInstant ? ((DateTime)x.apply(t)).getMillis() : x.apply(t),
                                    newMap(SemiGroups.failUnequal(),
                                        flatMap(mkDataRows(values), zip(tail(members), categoryObjects)))));
                        }
                    }
                } else {
                    // y has at least one member, x is anything else -> chart counts
                    if (xIsListOfValuesFromASingleRow) {
                        Iterable<Iterable<Object>> allRows = map(ChartConversionService_.handleCollections, sequence(Assert.singleton(objs), members));
                        Iterable<Function1<Object, Object>> tailMembers = repeat(Function.id(), size(tail(members)));
                        for (Iterable<Object> category: transpose(allRows)) {
                            List<Object> categoryObjects = newList(tail(category));
                            data.add(with(SemiGroups.failUnequal(), "c", head(category),
                                         reduce(Monoids.mapCombine(SemiGroups.longSum),
                                             map(categoryValues(categoryObjects), tailMembers))));
                        }
                    } else {
                        for (Object category: xValues) {
                            List<T> categoryObjects = newList(filter(x.andThen(equalTo(category)), objs));
                            data.add(with(SemiGroups.failUnequal(), "c", category,
                                         reduce(Monoids.mapCombine(SemiGroups.longSum),
                                             map(categoryValues(categoryObjects), tail(members)))));
                        }
                    }
                }
            }
        }
        
        String jsonData = new String(json.serialize(data), StandardCharsets.UTF_8).replace("\n", "");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream(32000);
        OutputStreamWriter ow = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        HtmlCanvas html = HttpServletCanvas.of(request.getHttpServletRequest(), ow);
        String contextPath = ((HttpServletCanvas<?>)html).getContextPath();
        try {
            html.html()
                .render(DocType.HTML5)
                .head()
                  .render(pageHead(title, jsonData, yNames, xIsTemporal, isStacked, isGrouped, xIsLinear, xIsInterval))
                ._head()
                .body()
                  .div(id("chart"))._div()
                ._body()
                .script(type("text/javascript").src(contextPath + "/r/js/lib/amcharts.min.js"))._script()
                .script(type("text/javascript").src(contextPath + "/r/js/lib/amcharts-xy.min.js"))._script()
                .script(type("text/javascript").src(contextPath + "/r/js/lib/amcharts-Animated.min.js"))._script()
                .script(type("text/javascript").src(contextPath + "/r/js/lib/amcharts-locale-fi_FI.min.js"))._script()
                .script(type("text/javascript")).write(scripts(jsonData, yNames, xIsTemporal, isStacked, isGrouped, xIsLinear, xIsInterval), false)._script()
                .script(type("text/javascript")).write(scripts2(), false)._script()
              ._html();

            ow.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return os.toByteArray();
    }
    
    protected Renderable pageHead(final String title, final String jsonData, final Collection<String> yNames, boolean isTemporal, boolean isStacked, boolean isGrouped, boolean isNumeric, boolean xIsInterval) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                    .meta(http_equiv("Content-Type").content("text/html;charset=UTF-8"))
                    .meta(http_equiv("Content-Security-Policy").content("default-src 'self';script-src 'self' 'unsafe-eval' '" + UI.calculateHash(scripts2()) + "' '" + UI.calculateHash(scripts(jsonData, yNames, isTemporal, isStacked, isGrouped, isNumeric, xIsInterval)) + "'"))
                    .title().write(title)._title();
            }
        };
    };

    private final String scripts(String jsonData, final Collection<String> yNames, boolean xIsTemporal, boolean isStacked, boolean isGrouped, boolean xIsLinear, boolean xIsInterval) {
        return  "let data = " + jsonData + ";\n"
              + "let root = am5.Root.new('chart', {"
              + "  timezone: am5.Timezone.new('Europe/Helsinki'),\n"
              + "  locale: am5locales_fi_FI"
              + "});\n"
              + "root.setThemes([am5themes_Animated.new(root)]);\n"
              + "let chart = root.container.children.push(\n"
              + "  am5xy.XYChart.new(root, {\n"
              + "    layout: root.verticalLayout,\n"
              + "  })\n"
              + ");\n"
              + "let yAxis = chart.yAxes.push(\n"
              + "  am5xy.ValueAxis.new(root, {\n"
              + "    calculateTotals: true,\n"
              + (isGrouped ? "    maxPrecision:0,\n" : "")
              + (isGrouped ? "    min: 0,\n" : "")
              + "    renderer: am5xy.AxisRendererY.new(root, {})\n"
              + "  })\n"
              + ");\n"
              + "yAxis.get('renderer').labels.template.adapters.add('text', function(text, target) {\n"
              + "  let ret = data.find(x => Object.entries(x).find(e => e[0].startsWith('_') && (''+e[1]) == text));\n"
              + "  return ret ? ret[Object.entries(ret).find(e => e[0].startsWith('_') && (''+e[1]) == text)[0].substring(1)] : text;\n"
              + "});\n"
              + "let xRenderer = am5xy.AxisRendererX.new(root, {"
              + (xIsLinear ? "" : "  minGridDistance: 0")
              + "});\n"
              + "xRenderer.labels.template.setAll({\n"
              + "  rotation: 75,\n"
              + "  centerY: am5.p50\n"
              + "});\n"
              + "let xAxis = chart.xAxes.push(\n"
              + (xIsTemporal
                  ? "am5xy.DateAxis.new(root, {"
              + "        groupData: " + isGrouped + ",\n"
              + "        groupCount: 2000,\n"
              + "        baseInterval: { timeUnit: 'second', count: 1 },\n"
              + "        dateFormats: {\n"
              + "            'second': 'HH:mm:ss',\n"
              + "            'minute': 'HH:mm',\n"
              + "            'hour': 'EEE HH:mm',\n"
              + "            'day': 'EEE d.M',\n"
              + "            'week': 'd.M',\n"
              + "            'month': 'MMM',\n"
              + "            'year': 'yyyy'\n"
              + "        },\n"
              + "        periodChangeDateFormats: {\n"
              + "            'second': 'HH:mm:ss',\n"
              + "            'minute': 'EEE HH:mm',\n"
              + "            'hour': 'EEE HH:mm (d.M.)',\n"
              + "            'day': 'd.M.yyyy',\n"
              + "            'week': 'd.M',\n"
              + "            'month': 'MMM yyyy',\n"
              + "            'year': 'yyyy'\n"
              + "        },\n"
              + "        groupIntervals: [\n"
              + "          { timeUnit: 'second', count: 1 },\n"
              + "          { timeUnit: 'hour', count: 1 },\n"
              + "          { timeUnit: 'day', count: 1 },\n"
              + "          { timeUnit: 'week', count: 1 },\n"
              + "          { timeUnit: 'month', count: 1 },\n"
              + "          { timeUnit: 'month', count: 3 },\n"
              + "          { timeUnit: 'year', count: 1 }\n"
              + "        ],\n"
                  : xIsLinear
                  ? "am5xy.ValueAxis.new(root, {\n"
                  : "am5xy.CategoryAxis.new(root, { categoryField: 'c',\n")
              + "    renderer: xRenderer,\n"
              + "    tooltip: am5.Tooltip.new(root, {}),\n"
              + "  })\n"
              + ");\n"
              + (xIsLinear? "chart.set('scrollbarX', am5.Scrollbar.new(root, { orientation: 'horizontal' }));\n" : "")
              + "xAxis.data.setAll(data);\n"
              + (xIsTemporal && isGrouped ?
                "let buttons = chart.plotContainer.children.push(am5.Container.new(root, {\n"
              + "  layout:  root.horizontalLayout"
              + "}));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ei ryhmittelyä',\n"
              + "  label: am5.Label.new(root, {text:'-'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'second', count: 1 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely vuosittain',\n"
              + "  label: am5.Label.new(root, {text:'y'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'year', count: 1 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely kvartaaleittain',\n"
              + "  label: am5.Label.new(root, {text:'q'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'month', count: 3 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely kuukausittain',\n"
              + "  label: am5.Label.new(root, {text:'m'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'month', count: 1 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely viikoittain',\n"
              + "  label: am5.Label.new(root, {text:'w'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'week',  count: 1 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely päivittäin',\n"
              + "  label: am5.Label.new(root, {text:'d'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'day',   count: 1 }));\n"
              + "buttons.children.push(am5.Button.new(root, {\n"
              + "  tooltipText: 'Ryhmittely tunneittain',\n"
              + "  label: am5.Label.new(root, {text:'h'})\n"
              + "})).events.on('click', ev => xAxis.set('groupInterval', { timeUnit: 'hour',   count: 1 }));\n"
                : "")
              + mkString("", map(new Apply<Pair<Integer,String>,String>() {
                @Override
                public String apply(Pair<Integer,String> yName) {
                    return "let series" + yName.left() + " = chart.series.push(\n"
                         + (xIsLinear ? (
                            (isGrouped ? "am5xy.StepLineSeries.new(root, {connect:" + xIsInterval + ",\n" : "am5xy.LineSeries.new(root, {\n")
                           + "    valueXField: 'c',\n"
                           + (isGrouped && !xIsInterval ? "valueYGrouped: 'sum',\nvalueYShow: 'valueYTotal',\n"
                                        : "")
                           + "    tooltip: am5.Tooltip.new(root, { pointerOrientation: 'horizontal', labelText: '" + yName.right() + ": {" + (isGrouped ? "valueY" : yName.right()) + "}\\n{valueX" + (xIsTemporal ? ".formatDate(\"yyyy-MM-dd HH:mm:ss\")" : "") + "}' }),\n"
                             ) : (
                             "  am5xy.ColumnSeries.new(root, {\n"
                           + "    categoryXField: 'c',\n"
                           + "    tooltip: am5.Tooltip.new(root, { pointerOrientation: 'horizontal', labelText: '" + yName.right() + ": {" + yName.right() + "}\\n{categoryX}' }),\n"
                         ))
                         + "    name: '" + yName.right() + "',\n"
                         + "    xAxis: xAxis,\n"
                         + "    yAxis: yAxis,\n"
                         + "    baseAxis: xAxis,\n"
                         + "    valueYField: '" + (isGrouped ? "" : "_") + yName.right() + "',\n"
                         + "    stacked: " + isStacked + ",\n"
                         + "    snapTooltip: true,\n"
                         + "  })\n"
                         + ");\n"
                         + (xIsLinear
                             ? "series" + yName.left() + ".fills.template.setAll({fillOpacity: 0.5, visible: true});\n"
                             : "series" + yName.left() + ".columns.template.set('tooltipText', '" + yName.right() + "\\n{valueY}');\n")
                         + "series" + yName.left() + ".data.setAll(data);\n";
                }}, zipWithIndex(yNames)))
              + (xIsLinear || !isStacked ? "" :
                    "series" + (yNames.size()-1) + ".bullets.push(function () {\n"
                  + "  return am5.Bullet.new(root, {\n"
                  + "    locationY: 1,\n"
                  + "    sprite: am5.Label.new(root, {\n"
                  + "      text: '{valueYTotal}',\n"
                  + "      fill: am5.color(0x000000),\n"
                  + "      centerY: am5.p100,\n"
                  + "      centerX: am5.p50,\n"
                  + "      populateText: true\n"
                  + "    })\n"
                  + "  });\n"
                  + "});\n")
              + "let legend = chart.children.push(am5.Legend.new(root, {}));\n"
              + "legend.data.setAll(chart.series.values);\n"
              + "chart.set('cursor', am5xy.XYCursor.new(root, {behavior: 'zoomX'}));";
    }
    
    private final String scripts2() {
        return "window.addEventListener('load', function() {"
            + "    let m = window.location.href.match(/\\/[0-9.]+\\/([0-9]+)\\//);"
            + "    window.history.replaceState(undefined, undefined, window.location.href.replace(/(\\/[0-9.]+\\/)[0-9]+\\//,(_,x) => x));"
            + "});";
    }
}
