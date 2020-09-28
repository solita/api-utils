package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Function.__;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.sequence;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;
import static fi.solita.utils.functional.Transformers.append;
import static fi.solita.utils.functional.Transformers.prepend;
import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.http_equiv;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.onClick;
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

import javax.servlet.http.HttpServletRequest;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.ext.spring.HtmlCanvasFactory;

import fi.solita.utils.api.base.html.HtmlModule;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Count_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Function;
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
        return serialize(title, tableHeader(members), regularBody(obj, members), request);
    }
    
    public <K,V> byte[] serialize(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(title, tableHeader(members), regularBody(flatten(obj.values()), members), request);
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
        return serialize(title, tableHeader(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members), request);
    }
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithKey(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<? super V, ?>> members, final MetaNamedMember<? super V,?> key) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        members = filter(not(equalTo((MetaNamedMember<V,Object>)key)), (Iterable<MetaNamedMember<V,Object>>)members);
        headers = cons((MetaNamedMember<V,Object>)key, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(title, tableHeader(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members), request);
    }
    
    protected String extraStyle() {
        return "";
    }
    
    protected Renderable pageHead(final HtmlTitle title) {
        return new Renderable() {
            @SuppressWarnings("unchecked")
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                    .meta(http_equiv("Content-Type").content("text/html;charset=UTF-8"))
                    .title().write(title.plainTextTitle)._title()
                    .style().write(
                              "html,h1,table  { font-family: sans-serif; font-weight: lighter; }"
                            + "body           { margin: 0; display: flex; flex-direction: column; padding: 0 0.5em; min-height: 100%; height: 100%; overflow-x: hidden; }"
                            
                            + "header, footer { display: flex; padding: 0.5em; }"
                            + "h1             { flex: 1; }"
                            + "header .page   { padding-left: 1em; }"
                            + "header .type-datetime, header .type-oid, header .type-rectangle { font-size: small; font-style: italic; padding: 1em; }"
                            + "header .type-rectangle { color: #bbb; }"
                            + ".lang li       { display: inline; padding: 0 1em; border-width: 0 0 0 1px; border-style: dotted; cursor: pointer; }"
                            
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
                            
                            + "body.en .fi, body.fi .en { display: none !important; }"
                            + ".fi i, .en i   { font-weight: normal; display: block; }"
                            + "*[title]::after { content: '?'; font-size: 0.75em; font-style: italic; color: #bbb; font-weight: lighter; }"
                            
                            + "a:hover        { position: relative; }"
                            + "a:hover iframe { visibility: visible !important; transition: visibility; transition-delay: 0.5s; z-index: 999; }"
                            + "td:first-child iframe { left: 0; right: auto; }"
                            + "iframe         { position: absolute; right: 0; visibility: hidden; margin-top: 1px; height: 45em; width: 45em; z-index: -1; background: white; border: 1px solid #ddd; }"
                            
                            + "td.null        { background: #f8f8f8; }"
                            + ".type-interval, .type-point, .type-kmetaisyys, .type-ratakmetaisyys, .type-ratakmvali { white-space: nowrap; }"
                            + ".type-multiline, .type-multipolygon { overflow: hidden; height: 1em; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; }"
                            + ".type-multiline:hover, .type-multipolygon:hover { overflow: visible; height: auto; display: inline; }"
                            + mkString(" ", sequence(
                                 ("  h1             { font-size: 1.4em; margin-bottom: 0.1em; }"
                                + "  h1 .type-datetime { display: block; }"
                                + "  table          { display: flex; position: relative; width: 100%; white-space: nowrap; counter-reset: none; }"
                                + "  table tr       { border-top: none; counter-increment: none; }"
                                + "  thead          { display: inline-block; font-size: small; max-width: 33%; overflow-x: auto; }"
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
                                + "  .fi i, .en i   { display: inline; padding-left: 0.25em; margin-left: 0.25em; border-left: 1px solid #eee; }"),
                             newList((Apply<String,String>)(Object)prepend("@media only screen and (max-width: 800px) {").andThen(append("}")), (Apply<String,String>)HtmlConversionService_.split.apply(Function.__, "}").andThen(HtmlConversionService_.<String>toList()).andThen(Transformers.map(prepend(".nested ").andThen(HtmlConversionService_.replaceAll.apply(__, ",", ", .nested ")).andThen(append("}")))).andThen(HtmlConversionService_.join.ap(" ")) )))
                                    
                            + extraStyle()
                          , false)
                    ._style()
                    .script(type("text/javascript"))
                        .cdata()
                            .write("window.iframeLoad = function(parent) {"
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
                                    + "};", false)
                        ._cdata()    
                    ._script()
                    .script(type("text/javascript"))
                        .cdata()
                            /*.write("var prevScrollY = window.pageYOffset || document.documentElement.scrollTop;" +
                                   "var scrollY = prevScrollY;" +
                                   "window.addEventListener('scroll', function() {" +  
                                   "    if (!document.body.classList.contains('disable-hover')) {" +
                                   "      document.body.classList.add('disable-hover');" +
                                   "    }" +
                                   "}, false);" +
                                   "setInterval(function() {" +
                                   "  if (prevScrollY == scrollY && scrollY == (window.pageYOffset || document.documentElement.scrollTop)) {" +
                                   "    document.body.classList.remove('disable-hover');" +
                                   "  }" +
                                   "  prevScrollY = scrollY;" +
                                   "  scrollY = window.pageYOffset || document.documentElement.scrollTop;" +
                                   "}, 1000);", false)*/
                        ._cdata()
                    ._script()
                    .script(type("text/javascript"))
                        .cdata()
                            .write("window.loadContent = function(url, parent) {" +
                                   "  var temp = document.createElement('div');" +
                                   "  var x = new XMLHttpRequest();" +
                                   "  x.onreadystatechange = function() {" +
                                   "    if (x.readyState == XMLHttpRequest.DONE ) {" +
                                   "      if (x.status == 200) { temp.innerHTML = x.responseText; parent.innerHTML = temp.getElementsByTagName('table')[0].outerHTML; parent.classList = ['nested'] }" +
                                   "      else { console.log(x.status); }" +
                                   "    }" +
                                   "  };" +
                                   "  x.open('GET', url);" +
                                   "  x.send();" +
                                   "};" +
                                   "window.loadAdditionalContent = function() { " +
                                   "  var links = document.body.getElementsByClassName('type-oid');" +
                                   "  for (var i = 0; i < links.length; ++i) {" +
                                   "    var link = links[i].parentElement;" +
                                   "    while (link.nodeName.toLowerCase() != 'td' && link.nodeName.toLowerCase() != 'body') { link = link.parentElement; }" +
                                   "    if (link.previousSibling != null && link.previousSibling.nodeName.toLowerCase() == 'td') {" +
                                   "      var as = link.getElementsByTagName('a');" +
                                   "      if (as.length == 1) {" +
                                   "        var url = as[0].attributes.href.value;" +
                                   "        loadContent(url, link);" +
                                   "      }" +
                                   "    }" +
                                   "  }" +
                                   "};", false)
                        ._cdata()
                    ._script();
            }
        };
    };
    
    public static Renderable pageHeader(final HtmlTitle title) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.header()
                        .h1()
                          .render(title)
                        ._h1()
                        .div(id("lang").class_("lang"))
                            .ul()
                                .li(onClick("document.body.classList.add('fi'); document.body.classList.remove('en')")).a().write("finnish")._a()._li()
                                .li(onClick("document.body.classList.add('en'); document.body.classList.remove('fi')")).a().write("english")._a()._li()
                            ._ul()
                        ._div()
                    ._header();
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
                    .span(class_("fi timestamps"))
                        .write("Aikaleimat Suomen aikavyöhykkeellä (EET)")
                    ._span()
                    .span(class_("en timestamps"))
                        .write("Timestamps in Europe/Helsinki (EET) time zone")
                    ._span()
                    .span(class_("fi copyright"))
                        .write("© " + copyright_fi)
                    ._span()
                    .span(class_("en copyright"))
                        .write("© " + copyright_en)
                    ._span()
                  ._footer();
            }
        };
    }
    
    private byte[] serialize(HtmlTitle title, Renderable tableHeader, Renderable tableBody, HttpServletRequest request) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(32000);
        OutputStreamWriter ow = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        HtmlCanvas html = HtmlCanvasFactory.createCanvas(request, null, ow);
        
        try {
            html.html()
                .render(DocType.HTML5)
                .head()
                  .render(pageHead(title))
                ._head()
                .body(class_("fi"))
                  .render(pageHeader(title))
                  .section(id("content"))
                      .table()
                        .thead()
                          .tr()
                            .render(tableHeader)
                          ._tr()
                        ._thead()
                        .tbody()
                          .render(tableBody)
                        ._tbody()
                      ._table()
                  ._section()
                  .render(pageFooter())
                ._body()
              ._html();
            
            ow.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return os.toByteArray();
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
                        .span(class_("fi " + extraClasses).title(pseudo ? null : docDescription(member.getMember()).getOrElse(null)))
                            .write(member.getName())
                        ._span();
                    html.span(class_("en " + extraClasses).title(pseudo ? null : docDescription_en(member.getMember()).getOrElse(null)))
                            .write(pseudo ? member.getName() : docName_en(member.getMember()).getOrElse(member.getName()))
                        ._span();
                    html._th();
                }
            }
        };
    }
    
    protected String extraClasses(AccessibleObject member) {
        return "";
    }
    
    protected abstract Option<String> docName_en(AccessibleObject member);

    protected abstract Option<String> docDescription_en(AccessibleObject member);

    protected abstract Option<String> docDescription(AccessibleObject member);

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
}
