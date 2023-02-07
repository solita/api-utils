package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Function.__;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.sequence;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.FunctionalC.drop;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;
import static fi.solita.utils.functional.Transformers.append;
import static fi.solita.utils.functional.Transformers.prepend;
import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;
import static org.rendersnake.HtmlAttributesFactory.http_equiv;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.lang;
import static org.rendersnake.HtmlAttributesFactory.name;
import static org.rendersnake.HtmlAttributesFactory.rowspan;
import static org.rendersnake.HtmlAttributesFactory.type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.AccessibleObject;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.ext.spring.HtmlCanvasFactory;

import fi.solita.utils.api.base.html.HtmlModule;
import fi.solita.utils.api.html.UI;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Count_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class HtmlConversionService {

    public static abstract class HtmlTitle implements Renderable {
        public final String plainTextTitle;

        public HtmlTitle(String plainTextTitle) {
            this.plainTextTitle = plainTextTitle;
        }
    }
    
    protected HtmlModule htmlModule;

    public HtmlConversionService(HtmlModule htmlModule) {
        this.htmlModule = htmlModule;
    }
    
    public HtmlTitle title(final String title) {
        return title(title, Option.<Count>None(), Option.<StartIndex>None());
    }
    
    public HtmlTitle title(final String title, final Option<Count> count, final Option<StartIndex> startIndex) {
        return new HtmlTitle(title) {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.span()
                        .write(title)
                    ._span();
                
                if (startIndex.isDefined() && !startIndex.get().equals(StartIndex.DEFAULT) && count.isDefined() && !count.get().equals(Count.DEFAULT)) {
                    html.span(class_("page"))
                            .write("(" + Integer.toString(startIndex.get().value) + "-" + count.map(Count_.value.andThen(HtmlConversionService_.plus.ap(startIndex.get().value-1).andThen(Transformers.toString))).getOrElse("") + ")")
                        ._span();
                } else {
                    for (StartIndex si: startIndex) {
                        if (!si.equals(StartIndex.DEFAULT)) {
                            html.span(class_("page"))
                                    .write("(" + Integer.toString(si.value) + "- )")
                                ._span();
                        }
                    }
                    if (!startIndex.isDefined()) {
                        for (Count c: count) {
                            if (!c.equals(Count.DEFAULT)) {
                                html.span(class_("page"))
                                        .write("(1-" + Integer.toString(c.value) + ")")
                                    ._span();
                            }
                        }
                    }
                }
            }
        };
    }
    
    static int plus(int a, int b) {
        return a + b;
    }
    
    public <T> byte[] serialize(HttpServletRequest request, HtmlTitle title, T obj, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return serialize(request, title, newList(obj), members);
    }
    
    public <T> byte[] serialize(HttpServletRequest request, HtmlTitle title, T[] obj) {
        return serialize(request, title, newList(obj));
    }
    
    public <T> byte[] serialize(HttpServletRequest request, HtmlTitle title, final Iterable<T> obj) {
        return serialize(request, title, newList(obj), newList(new MetaNamedMember<T, T>() {
            @Override
            public T apply(T t) {
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
        }));
    }
    
    public <T> byte[] serialize(HttpServletRequest request, HtmlTitle title, final Collection<T> obj, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return serialize(title, tableHeader(members), regularBody(obj, members), request, obj.size());
    }
    
    public <K,V> byte[] serialize(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(title, tableHeader(members), regularBody(flatten(obj.values()), members), request, obj.size());
    }
    
    public <K,V> byte[] serializeSingle(HttpServletRequest request, HtmlTitle title, final Map<K,V> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(title, tableHeader(members), regularBody(obj.values(), members), request, obj.size());
    }
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithKey(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        // empty header if there's no simple key. This is a bit too hackish...
        headers = cons(new MetaNamedMember<V, Object>() {
            @Override
            public Object apply(V t) {
                throw new UnsupportedOperationException();
            }
            @Override
            public AccessibleObject getMember() {
                throw new UnsupportedOperationException();
            }
            @Override
            public String getName() {
                return "";
            }
        }, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(title, tableHeader(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members), request, obj.size());
    }
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithKey(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<? super V, ?>> members, final MetaNamedMember<? super V,?> key) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        members = filter(not(equalTo((MetaNamedMember<V,Object>)key)), (Iterable<MetaNamedMember<V,Object>>)members);
        headers = cons((MetaNamedMember<V,Object>)key, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(title, tableHeader(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members), request, obj.size());
    }
    
    protected String extraStyle() {
        return "";
    }
    
    protected Renderable pageHead(final HtmlTitle title, final HttpServletRequest request) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                    .meta(http_equiv("Content-Type").content("text/html;charset=UTF-8"))
                    .meta(http_equiv("Content-Security-Policy").content("default-src 'self';style-src 'self' '"
                        + UI.calculateHash(styles()) +"' 'sha256-/jDKvbQ8cdux+c5epDIqkjHbXDaIY8RucT1PmAe8FG4=';script-src 'self' '"
                        + UI.calculateHash(scripts())+ "' '"
                        + UI.calculateHash(scripts2()) + "' '"
                        + UI.calculateHash(scripts3(request)) + "'"))
                    .meta(name("htmx-config").content("{ \"includeIndicatorStyles\": false }"))
                    .title().write(title.plainTextTitle)._title()
                    .style()
                    .write(styles(), false)
                    ._style()
                    .script(type("text/javascript").src(request.getContextPath() + "/r/tablefilter/tablefilter.js"))._script()
                    .script(type("text/javascript"))
                        .write(scripts(), false)
                    ._script();
            }
        };
    };
    
    public static Renderable pageHeader(final HtmlTitle title, final HttpServletRequest request, final boolean includeFormats) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.render(UI.langSelectorInput)
                    .header()
                        .h1()
                          .render(title)
                        ._h1()
                        .if_(includeFormats)
                            .div(class_("formats"))
                                .render(linksForDifferentFormats(request))
                            ._div()
                        ._if()
                        .render(UI.langSelectorLabel)
                    ._header();
            }
        };
    }
    
    private static Renderable linksForDifferentFormats(final HttpServletRequest request) {
        final String path = RequestUtil.getApiVersionBasePath(request) + drop(1, RequestUtil.getAPIVersionRelativePathWithoutRevision(request));
        final String queryString = Option.of(request.getQueryString()).map(Transformers.prepend("?")).getOrElse("");
        
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                for (String format: newList("html","json","jsonl","geojson","csv","xlsx")) {
                    html.a(href(path.replace(".html", "." + format) + queryString))
                            .write(format)
                        ._a();
                }
            }
        };
    }
    
    protected Renderable pageFooter() {
        return pageFooter("?", "?");
    }
    
    protected Renderable pageFooter(final String copyright_fi, final String copyright_en) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                  .footer()
                    .span(lang("fi").class_("timestamps"))
                        .write("Aikaleimat Suomen aikavyöhykkeellä (EET)")
                    ._span()
                    .span(lang("en").class_("timestamps"))
                        .write("Timestamps in Europe/Helsinki (EET) time zone")
                    ._span()
                    .span(lang("fi").class_("copyright"))
                        .write("© " + copyright_fi)
                    ._span()
                    .span(lang("en").class_("copyright"))
                        .write("© " + copyright_en)
                    ._span()
                  ._footer();
            }
        };
    }
    
    private byte[] serialize(HtmlTitle title, Renderable tableHeader, Renderable tableBody, HttpServletRequest request, int rows) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(32000);
        OutputStreamWriter ow = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        HtmlCanvas html = HtmlCanvasFactory.createCanvas(request, null, ow);
        
        try {
            html.html()
                .render(DocType.HTML5)
                .head()
                  .render(pageHead(title, request))
                ._head()
                .body(rows == 1 ? class_("singleton") : null)
                  .render(pageHeader(title, request, true))
                  .section(id("content"))
                      .table(id("table"))
                        .thead()
                          .tr()
                            .render(tableHeader)
                          ._tr()
                        ._thead()
                        .tbody()
                          .render(tableBody)
                        ._tbody()
                      ._table()
                      .if_(rows > 0 && COUNT.matcher(Option.of(request.getQueryString()).getOrElse("")).matches())
                          .render(initHtmx(request))
                          .div(class_("lds-dual-ring"))._div()
                          .div(class_("load-more")
                                  .add("hx-push-url", "false")
                                  .add("hx-boost", "true")
                                  .add("hx-target", "#content > table > tbody")
                                  .add("hx-swap", "beforeend")
                                  .add("hx-indicator", ".lds-dual-ring")
                                  .add("hx-select", "#content > table > tbody > tr"))
                              .a(href(uriWithIncrementedStartIndex(request, false)))
                                  .span(lang("fi")).write("Lataa lisää rivejä...")._span()
                                  .span(lang("en")).write("Load more rows...")._span()
                              ._a()
                              .a(href(uriWithIncrementedStartIndex(request, true)))
                                  .span(lang("fi")).write("Lataa loput rivit...")._span()
                                  .span(lang("en")).write("Load rest of the rows...")._span()
                              ._a()
                          ._div()
                      ._if()
                  ._section()
                  .render(pageFooter())
                  .script(type("text/javascript"))
                      .write(scripts3(request), false)
                  ._script()
                ._body()
              ._html();
            
            ow.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return os.toByteArray();
    }
    
    private static Renderable initHtmx(final HttpServletRequest request) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.script(type("text/javascript").src(request.getContextPath() + "/r/js/lib/htmx.min.js"))._script()
                    .script(type("text/javascript"))
                        .write(scripts2(), false)
                    ._script();
            }
        };
    }
    
    public static final Pattern COUNT = Pattern.compile("count=([0-9]+)(.*)");
    public static final Pattern START_INDEX = Pattern.compile("startIndex=([0-9]+)");
    
    static String uriWithIncrementedStartIndex(HttpServletRequest request, boolean loadRest) {
        Matcher cm = COUNT.matcher(Option.of(request.getQueryString()).getOrElse(""));
        if (cm.find()) {
            int count = Integer.parseInt(cm.group(1));
            
            String uri = RequestUtil.getApiVersionBasePath(request) + drop(1, RequestUtil.getAPIVersionRelativePathWithoutRevision(request)) + Option.of(request.getQueryString()).map(Transformers.prepend("?")).getOrElse("");
            StringBuilder sb = new StringBuilder();
            Matcher m = START_INDEX.matcher(uri);
            if (m.find()) {
                m.appendReplacement(sb, "startIndex=" + (Integer.parseInt(m.group(1)) + count));
                m.appendTail(sb);
            } else {
                m.appendTail(sb);
                sb.append("&startIndex=" + (count+1));
            }
            
            if (loadRest) {
                return COUNT.matcher(sb.toString()).replaceFirst("$2").replace("?&", "?");
            } else {
                return sb.toString();
            }
        } else {
            return "";
        }
    }
    
    static String[] split(String s, String regex) {
        return s.split(regex);
    }
    
    static String replaceAll(String str, String regex, String replacement) {
        return str.replaceAll(regex, replacement);
    }
    
    public static <T> List<T> toList(T[] ts) {
        return newList(ts);
    }

    public static String join(String separator, Iterable<String> ss) {
        return mkString(separator, ss);
    }
    
    private <T> Renderable tableHeader(final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                for (MetaNamedMember<T, ?> member: members) {
                    boolean pseudo = member.getName().isEmpty();
                    String extraClasses = pseudo ? "" : extraClasses(member.getMember());
                    html.th()
                        .span(lang("fi").class_(extraClasses).title(pseudo ? null : docDescription(member).getOrElse(null)))
                            .write(pseudo ? member.getName() : docName(member).getOrElse(member.getName()))
                        ._span();
                    html.span(lang("en").class_(extraClasses).title(pseudo ? null : docDescription_en(member).getOrElse(null)))
                            .write(pseudo ? member.getName() : docName_en(member).getOrElse(member.getName()))
                        ._span();
                    html._th();
                }
            }
        };
    }
    
    protected String extraClasses(AccessibleObject member) {
        return "";
    }
    
    protected abstract Option<String> docName(MetaNamedMember<?, ?> member);
    
    protected abstract Option<String> docName_en(MetaNamedMember<?, ?> member);

    protected abstract Option<String> docDescription(MetaNamedMember<?, ?> member);
    
    protected abstract Option<String> docDescription_en(MetaNamedMember<?, ?> member);

    private final <T> Renderable regularBody(final Iterable<T> obj, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                for (final T t: obj) {
                    html.tr()
                          .render(tablecols(t, members))
                        ._tr();
                }
            }
        };
    }

    private <K,V,O> Renderable mapBody(final Map<K, ? extends Iterable<V>> obj, final Iterable<? extends MetaNamedMember<V, O>> members) {
        return new Renderable() {
              @Override
              public void renderOn(HtmlCanvas html) throws IOException {
                  for (final Entry<K, ? extends Iterable<V>> t: obj.entrySet()) {
                      final List<V> values = newList(t.getValue());
                      html.tr()
                          .render(new Renderable() {
                            @Override
                            public void renderOn(HtmlCanvas html) throws IOException {
                                html.td(rowspan(Integer.toString(values.size())))
                                      .render(htmlModule.toRenderable(t.getKey()))
                                    ._td();
                                if (!values.isEmpty()) {
                                    html.render(tablecols(head(values), members));
                                }
                            }
                          })
                          ._tr()
                          .render(regularBody(tail(values), members));
                  }
              }
          };
    }
    
    private <T> Renderable tablecols(final T t, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                for (MetaNamedMember<T, ?> member: members) {
                    Object val = member.apply(t);
                    html.td(class_(val == null ? "null" : null))
                          .render(htmlModule.toRenderable(val))
                        ._td();
                }
            }
          };
    }
    
    @SuppressWarnings("unchecked")
    private final String styles() {
        return
          "html,h1,table  { font-family: sans-serif; font-weight: lighter; }"
        + "body           { margin: 0; display: flex; flex-direction: column; padding: 0 0.5em; min-height: 100%; height: 100%; overflow-x: hidden; }"
        
        + "header, footer { display: flex; padding: 0.5em; }"
        + "h1             { flex: 1; }"
        + "header .page   { padding-left: 1em; }"
        + "header .type-datetime, header .type-oid, header .type-rectangle { font-size: small; font-style: italic; padding: 1em; }"
        + "header .type-rectangle { color: #bbb; }"
        + ".lang-selector { display: inline; padding: 0 1em; border-width: 0 0 0 1px; border-style: dotted; cursor: pointer; }"
        
        + "footer *       { color: #ccc; font-size: small; font-style: italic; flex: 1; }"
        + ".timestamps    { text-align: left; }"
        + ".copyright     { text-align: right; }"
        
        + "section        { flex: 1; overflow-y: auto; }"
        + "table          { border-collapse: collapse; counter-reset: rowNumber; }"
        + "th,td          { vertical-align: top; padding: 0.5em 1em 0.5em 0; }"
        + "th             { position: -webkit-sticky; position: sticky; top: 0; z-index: 999; background-color: white; text-align: left; }"
        + "tbody          { font-size: small; }"
        + "tbody tr       { border-top: 1px dotted #ccc; counter-increment: rowNumber; }"
        + "thead > tr > th:first-child,tbody > tr > td:first-child { margin-right: 1em; }"
        + "tbody > tr > td:first-child::before { content: counter(rowNumber); margin-right: 1em; color: #ddd; }"
        
        + "input.flt { height: 20px; }"
        + "table.TF tr th { padding-top: 0.5em; }"
        
        + "ul             { list-style: none; padding: 0; margin: 0; white-space: normal; }"
        + "ul li          { border-top: 1px dotted #ddd; }"
        + "ul li:first-child { border: none; }"
        + "ul li.i-1  { background-color: #fffafa; }"
        + "ul li.i-2  { background-color: #f0fff0; }"
        + "ul li.i-3  { background-color: #f5fffa; }"
        + "ul li.i-4  { background-color: #f0ffff; }"
        + "ul li.i-5  { background-color: #f0f8ff; }"
        + "ul li.i-6  { background-color: #f8f8ff; }"
        + "ul li.i-7  { background-color: #f5f5f5; }"
        + "ul li.i-8  { background-color: #fff5ee; }"
        + "ul li.i-9  { background-color: #f5f5dc; }"
        + "ul li.i-10 { background-color: #fdf5e6; }"
        + "ul li.i-11 { background-color: #fffaf0; }"
        + "ul li.i-12 { background-color: #fffff0; }"
        + "ul li.i-13 { background-color: #faebd7; }"
        + "ul li.i-14 { background-color: #faf0e6; }"
        + "ul li.i-15 { background-color: #fff0f5; }"
        + "ul li.i-16 { background-color: #ffe4e1; }"
        + "ul li.i-17 { background-color: #fffafa; }"
        + "ul li.i-18 { background-color: #f0fff0; }"
        + "ul li.i-19 { background-color: #f5fffa; }"
        + "ul li.i-20 { background-color: #f0ffff; }"
        + "ul li.i-21 { background-color: #f0f8ff; }"
        + "ul li.i-22 { background-color: #f8f8ff; }"
        + "ul li.i-23 { background-color: #f5f5f5; }"
        + "ul li.i-24 { background-color: #fff5ee; }"
        + "ul li.i-25 { background-color: #f5f5dc; }"
        + "ul li.i-26 { background-color: #fdf5e6; }"
        + "ul li.i-27 { background-color: #fffaf0; }"
        + "ul li.i-28 { background-color: #fffff0; }"
        + "ul li.i-29 { background-color: #faebd7; }"
        + "ul li.i-30 { background-color: #faf0e6; }"
        + "ul li.i-31 { background-color: #fff0f5; }"
        + "ul li.i-32 { background-color: #ffe4e1; }"
        + "ul li ul li    { border: none !important; }"
        + "ul li ul li    { background-color: transparent !important; }"
        
        + "table table { counter-reset: none; }"
        + "table table tr { border: none; counter-increment: none; }"
        + "table table th { border: none; padding: 1px 3px; text-align: left; background: none; vertical-align: top; }"
        + "table table td { border: none; padding: 1px 3px; line-height: 1em; }"
        
        + UI.langSelectorCSS
        
        + "*[title]::after { content: '?'; font-size: 0.75em; font-style: italic; color: #bbb; font-weight: lighter; }"
        
        + "a:hover        { position: relative; }"
        + "a:hover iframe { visibility: visible !important; transition: visibility; transition-delay: 0.5s; z-index: 999; }"
        + "td:first-child iframe { left: 0; right: auto; }"
        + "iframe         { position: absolute; right: 0; visibility: hidden; margin-top: 1px; height: 45em; width: 45em; z-index: -1; background: white; border: 1px solid #ddd; }"
        
        + "td.null        { background: #f8f8f8; }"
        + ".type-interval, .type-point, .type-kmetaisyys, .type-ratakmetaisyys, .type-ratakmvali { white-space: nowrap; }"
        + ".type-multiline, .type-multipolygon { overflow: hidden; height: 1em; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; }"
        + ".type-multiline:hover, .type-multipolygon:hover { overflow: visible; height: auto; display: inline; }"
        
        + ".load-more     { padding-top: 1em; }"
        + ".load-more > * { white-space: nowrap; padding: 0 1em; font-style: italic; font-size: 0.75em; }"
        
        + ".formats       { position: absolute; }"
        + ".formats a     { padding: 0 1em; font-size: 0.75em; }"
        
        // spinner
        + ".htmx-request.lds-dual-ring { display: inline-block; }"
        + ".lds-dual-ring { display: none; width: 30px; height: 30px; margin-right: auto; margin-left: auto; }"
        + ".lds-dual-ring:after { content: ' '; display: block; width: 14px; height: 14px; margin: 8px; border-radius: 50%; border: 6px solid #000; border-color: #000 transparent #000 transparent; animation: lds-dual-ring 1.2s linear infinite; }"
        + "@keyframes lds-dual-ring { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }"
        
        + mkString(" ", sequence(
             ("  h1             { font-size: 1.4em; margin-bottom: 0.1em; }"
            + "  h1 .type-datetime { display: block; }"
            + "  table          { display: flex; position: relative; width: 100%; white-space: nowrap; counter-reset: none; }"
            + "  table tr       { border-top: none; counter-increment: none; }"
            + "  thead          { display: inline-block; font-size: small; max-width: 33%; min-width: 5em; overflow-x: auto; }"
            + "  tbody          { display: inline-block; font-size: small; min-width: 67%; white-space: nowrap; }"
            + "  thead tr       { display: block; }"
            + "  tbody tr       { display: flex; flex-direction: column; vertical-align: top; }"
            + "  tbody td       { overflow-x: auto; }"
            + "  tbody > tr > td:first-child::before { content: ''; margin-right: 0; }"
            + "  th, td         { display: block; height: 1.5em; min-height: 1.5em; max-height: 1.5em; text-align: left; border-bottom: 1px solid #eee; border-left: 1px solid #eee; padding: 0.25em; }"
            + "  th             { border-left: none; text-align: right; font-weight: normal; font-variant: small-caps; }"
            + "  td:first-child { border-left: 1px solid #eee; }"
            + "  td:last-child  { border-bottom: none; }"
            + "  ul             { white-space: nowrap; max-width: 20em; }"
            + "  table table tr { display: table-row; }"
            + "  table table td { display: table-cell; }"
            + "  table li       { display: inline-block; border-left: 1px solid #eee; padding: 0 3px; }"
            + "  table li:first-child { border-left: none; }"
            + "  .load-more     { display: none; }"),
         newList(HtmlConversionService_.prefixed.ap(".singleton").andThen((Apply<String,String>)(Object)prepend("@media only screen and (max-width: 800px) {")).andThen(append("}")),
                 HtmlConversionService_.prefixed.ap(".nested") )))
                
        + extraStyle();
    }
    
    static String prefixed(String prefix, String rules) {
        return join(" ", map(prepend(prefix + " ").andThen(HtmlConversionService_.replaceAll.apply(__, ",", ", " + prefix + " ")).andThen(append("}")), toList(rules.split("}"))));
    }

    private final String scripts() {
        return
              "window.iframeLoad = function(parent) {"
            + "var p = parent.parentElement;"
            + "while (p.nodeName.toLowerCase() != 'body' && p.nodeName.toLowerCase() != 'header') {"
            + "  p = p.parentElement;"
            + "}"
            + "if (p.nodeName.toLowerCase() == 'header') {"
            + "  return;"
            + "} else if (parent.getElementsByTagName('iframe').length == 0) {"
            + "  var elem = document.createElement('iframe');"
            + "  elem.style.display = 'none';"
            + "  parent.appendChild(elem);"
            + "  setTimeout(function() {"
            + "    if (window.getComputedStyle(elem).visibility == 'visible') {"
            + "      elem.setAttribute('src', parent.attributes.href.value);"
            + "      elem.style.display = 'block';"
            + "    } else {"
            + "      parent.removeChild(elem);"
            + "    }"
            + "  }, 1000);"
            + "}"
            + "};"
            + ""
            + "window.addEventListener('load', function() { "
            + "    let m = window.location.href.match(/\\/[0-9.]+\\/([0-9]+)\\//);"
            + "    if (m) {"
            + "        document.body.setAttribute('data-revision', m[1]);"
            + "    }"
            + "    window.history.replaceState(undefined, undefined, window.location.href.replace(/(\\/[0-9.]+\\/)[0-9]+\\//,(_,x) => x));"
            + ""
            + "    document.querySelectorAll('.type-oid a').forEach(function(e) { e.onmouseover = function() { window.iframeLoad(e); }; });"
            + "});"
            + ""
            + "window.loadContent = function(url, parent) {"
            + "  var temp = document.createElement('div');"
            + "  var x = new XMLHttpRequest();"
            + "  x.onreadystatechange = function() {"
            + "    if (x.readyState == XMLHttpRequest.DONE ) {"
            + "      if (x.status == 200) { temp.innerHTML = x.responseText; parent.innerHTML = temp.getElementsByTagName('table')[0].outerHTML; parent.classList = ['nested'] }"
            + "      else { console.log(x.status); }"
            + "    }"
            + "  };"
            + "  x.open('GET', url);"
            + "  x.send();"
            + "};"
            + ""
            + "window.loadAdditionalContent = function() { "
            + "  var links = document.body.getElementsByClassName('type-oid');"
            + "  for (var i = 0; i < links.length; ++i) {"
            + "    var link = links[i].parentElement;"
            + "    while (link.nodeName.toLowerCase() != 'td' && link.nodeName.toLowerCase() != 'body') { link = link.parentElement; }"
            + "    if (link.previousSibling != null && link.previousSibling.nodeName.toLowerCase() == 'td') {"
            + "      var as = link.getElementsByTagName('a');"
            + "      if (as.length == 1) {"
            + "        var url = as[0].attributes.href.value;"
            + "        loadContent(url, link);"
            + "      }"
            + "    }"
            + "  }"
            + "};";
    }
    
    public static final String scripts2() {
        return
             "if (htmx) { htmx.on('htmx:afterOnLoad', function(evt) {"
           + "    let m = evt.detail.xhr.responseURL.match(/\\/[0-9.]+\\/([0-9]+)\\//);"
           + "    if (m) {"
           + "        let newRevision = m[1];"
           + "        let oldRevision = document.body.getAttribute('data-revision');"
           + "        if (newRevision != oldRevision) {"
           // revision has changed, rows may not align properly anymore -> reload the page to not display invalid content.
           + "            window.location.reload();"
           + "        }"
           + "    }"
           + "});}";
    }
    
    public static final String scripts3(final HttpServletRequest request) {
        return "if (TableFilter) {"
             + "  new TableFilter('table', { auto_filter: { delay: 200 }, base_path: '" + request.getContextPath() + "/r/tablefilter/' }).init();"
             + "}";
    }
}
