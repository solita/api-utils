package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Function.__;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
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

import java.io.IOException;
import java.io.StringWriter;
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

import fi.solita.utils.api.base.HtmlModule;
import fi.solita.utils.api.types.Count;
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
    
    public static HtmlTitle title(final String title) {
        return title(title, Option.<Count>None());
    }
    
    public static HtmlTitle title(final String title, final Option<Count> count) {
        return new HtmlTitle(title) {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.span()
                        .write(title)
                    ._span();
                for (Count c: count) {
                    if (c.value != Integer.MAX_VALUE) {
                        html.span()
                                .write("1-" + Integer.toString(c.value))
                            ._span();
                    }
                }
            }
        };
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
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithoutKey(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
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
    public <K,V> byte[] serialize(HttpServletRequest request, HtmlTitle title, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<? super V, ?>> members, final MetaNamedMember<? super V,?> key) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        members = filter(not(equalTo((MetaNamedMember<V,Object>)key)), (Iterable<MetaNamedMember<V,Object>>)members);
        headers = cons((MetaNamedMember<V,Object>)key, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(title, tableHeader(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members), request);
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
                              "html           { font-family: sans-serif; font-weight: lighter; }"
                            + "body           { display: inline-block; }"
                            + "h1             { font-weight: lighter; margin-bottom: 0.3em; position: relative; padding-right: 1.5em; }"
                            + "h1 > span      { padding-left: 1em; }"
                            + "h1 .type-datetime { font-size: 0.5em; font-style: italic; }"
                            + "h1 .type-rectangle { font-size: 0.5em; font-style: italic; color: #bbb; }"
                            + "h1 .type-oid a { text-decoration: none; }"
                            + ".loadButton    { right: 0; position: absolute; padding: 0.5em; margin: 0.5em; visibility: hidden; }"
                            + ".lang          { right: 0; top:0; position: fixed; font-size: 0.5em; margin: 0.5em; }"
                            + ".lang li       { display: inline; padding: 0 1em; border-width: 0 0 0 1px; border-style: dotted; }"
                            + "section        { border-radius: 5px; border: 1px solid #ddd; overflow: auto; display: inline-block; }"
                            + "table          { border-collapse: collapse; border-spacing: 0; }"
                            + "thead          { background: #eee; vertical-align: top; }"
                            + "tbody          { font-size: 0.75em; overflow: visible; }"
                            + "th, td         { padding: 5px 10px; margin: 0; }"
                            + "th             { border-left: 1px solid #ccc; border-bottom: 1px solid #ccc; background-color: #ddd; }"
                            + "th:first-child { border-left: none; }"
                            + "td             { border-left: 1px dotted #eee; vertical-align: top; }"
                            + "td:first-child { border-left: none; }"
                            + "tr             { border-top: 1px solid #ccc; }"
                            + "tr:first-child { border-top: none; }"
                            + "footer         { padding-top: 10px; color: #ccc; font-style: italic; font-size: 0.8em; position: relative; }"
                            + ".copyright     { position: absolute; right: 0; padding: 0.5em; }"
                            + "ul             { list-style: none; padding: 0; margin: 0; white-space: normal; }"
                            + "ul li          { border-top: 1px solid #ddd; }"
                            + "ul li:first-child { border: none; }"
                            + "ul li.index-1  { background-color: #fffafa; }"
                            + "ul li.index-2  { background-color: #f0fff0; }"
                            + "ul li.index-3  { background-color: #f5fffa; }"
                            + "ul li.index-4  { background-color: #f0ffff; }"
                            + "ul li.index-5  { background-color: #f0f8ff; }"
                            + "ul li.index-6  { background-color: #f8f8ff; }"
                            + "ul li.index-7  { background-color: #f5f5f5; }"
                            + "ul li.index-8  { background-color: #fff5ee; }"
                            + "ul li.index-9  { background-color: #f5f5dc; }"
                            + "ul li.index-10 { background-color: #fdf5e6; }"
                            + "ul li.index-11 { background-color: #fffaf0; }"
                            + "ul li.index-12 { background-color: #fffff0; }"
                            + "ul li.index-13 { background-color: #faebd7; }"
                            + "ul li.index-14 { background-color: #faf0e6; }"
                            + "ul li.index-15 { background-color: #fff0f5; }"
                            + "ul li.index-16 { background-color: #ffe4e1; }"
                            + "ul li.index-17 { background-color: #fffafa; }"
                            + "ul li.index-18 { background-color: #f0fff0; }"
                            + "ul li.index-19 { background-color: #f5fffa; }"
                            + "ul li.index-20 { background-color: #f0ffff; }"
                            + "ul li.index-21 { background-color: #f0f8ff; }"
                            + "ul li.index-22 { background-color: #f8f8ff; }"
                            + "ul li.index-23 { background-color: #f5f5f5; }"
                            + "ul li.index-24 { background-color: #fff5ee; }"
                            + "ul li.index-25 { background-color: #f5f5dc; }"
                            + "ul li.index-26 { background-color: #fdf5e6; }"
                            + "ul li.index-27 { background-color: #fffaf0; }"
                            + "ul li.index-28 { background-color: #fffff0; }"
                            + "ul li.index-29 { background-color: #faebd7; }"
                            + "ul li.index-30 { background-color: #faf0e6; }"
                            + "ul li.index-31 { background-color: #fff0f5; }"
                            + "ul li.index-32 { background-color: #ffe4e1; }"
                            + "ul li ul li    { border: none !important; }"
                            + "ul li ul li    { background-color: transparent !important; }"
                            + "table table th { border: none; padding: 1px 3px; text-align: left; background: none; vertical-align: top; }"
                            + "table table td { border: none; padding: 1px 3px; line-height: 1em; }"
                            + "table table tr { border: none; }"
                            + "td.null        { background: #f8f8f8; }"
                            + "a              { position: relative; }"
                            + "a:hover iframe { display: block !important; }"
                            + "td:first-child iframe { left: 0; right: auto; }"
                            + "iframe         { position: absolute; right: 0; top: 1em; display: none; margin-top: 1px; height: 500px; width: 500px; z-index: 999; background: white; border: 1px solid #ddd; }"
                            + "body .fi, body .en { display: none !important; }"
                            + "body.fi .fi, body.en .en { display: inherit !important; }"
                            + ".fi i, .en i   { font-weight: normal; display: block; }"
                            + ".type-interval { white-space: nowrap; }"
                            + "*[title]::after { content: '?'; font-size: 0.75em; font-style: italic; float: right; margin-top: -0.25em; color: #bbb; font-weight: lighter; }"
                            + mkString(" ", sequence(
                                 ("  .loadButton    { display: none !important; }"
                                + "  h1             { font-size: 1.4em; margin-bottom: 0.1em; }"
                                + "  h1 .type-datetime { display: block; }"
                                + "  th, td         { vertical-align: top; display: block; line-height: 1.5em; }"
                                + "  th             { border-left: none; text-align: right; font-weight: normal; font-variant: small-caps; }"
                                + "  tr             { border-top: none; }"
                                + "  table          { display: flex; position: relative; width: 100%; overflow: auto; }"
                                + "  thead          { display: inline-block; font-size: 0.75em; }"
                                + "  tbody          { display: inline-block; width: auto; white-space: nowrap; }"
                                + "  thead tr       { display: block; }"
                                + "  tbody tr       { display: flex; flex-direction: column; vertical-align: top; }"
                                + "  ul             { white-space: nowrap; max-width: 20em; }"
                                + "  table table tr { display: table-row; }"
                                + "  table table td { display: table-cell; }"
                                + "  table li       { display: inline-block; border-left: 1px solid #eee; padding: 0 3px; }"
                                + "  table li:first-child { border-left: none; }"
                                + "  td, th         { min-height: 1.5em; text-align: left; border-bottom: 1px solid #eee; border-left: 1px solid #eee; }"
                                + "  td:last-child  { border-bottom: none; }"
                                + "  td:first-child { border-left: 1px solid #eee; }"
                                + "  .fi i, .en i   { display: inline; padding-left: 0.25em; margin-left: 0.25em; border-left: 1px solid #eee; }"),
                             newList((Apply<String,String>)(Object)prepend("@media only screen and (max-width: 800px) {").andThen(append("}")), (Apply<String,String>)HtmlConversionService_.split.apply(Function.__, "}").andThen(HtmlConversionService_.<String>toList()).andThen(Transformers.map(prepend(".nested ").andThen(HtmlConversionService_.replaceAll.apply(__, ",", ", .nested ")).andThen(append("}")))).andThen(HtmlConversionService_.join.ap(" ")) )))
                          , false)
                    ._style()
                    .script(type("text/javascript"))
                        .write("window.iframeLoad = function(parent) { var p = parent.parentElement; while (p.nodeName.toLowerCase() != 'body' && p.nodeName.toLowerCase() != 'header') { p = p.parentElement; } if (p.nodeName.toLowerCase() == 'header') { return; } else if (parent.getElementsByTagName('iframe').length == 0) { var elem = document.createElement('iframe'); elem.setAttribute('src', parent.attributes.href.value); parent.appendChild(elem); }; };", false)
                    ._script()
                    .script(type("text/javascript"))
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
                    ._script();
            }
        };
    };
    
    protected Renderable pageHeader(final HtmlTitle title) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                    .header()
                    .h1()
                      .render(title)
                      .button(id("loadButton").class_("loadButton").onClick("this.style.visibility = 'hidden'; window.location += '#deep'; loadAdditionalContent();"))
                          .write("Lataa lisäsisältö")
                      ._button()
                      .ul(id("lang").class_("lang"))
                          .li(onClick("document.body.classList.add('fi'); document.body.classList.remove('en')")).a().write("finnish")._a()._li()
                          .li(onClick("document.body.classList.add('en'); document.body.classList.remove('fi')")).a().write("english")._a()._li()
                      ._ul()
                    ._h1()
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
                    .span(class_("fi").class_("timestamps"))
                        .write("Aikaleimat Suomen aikavyöhykkeellä (EET)")
                    ._span()
                    .span(class_("en").class_("timestamps"))
                        .write("Timestamps in Europe/Helsinki (EET) time zone")
                    ._span()
                    .span(class_("fi").class_("copyright"))
                        .write("© " + copyright_fi)
                    ._span()
                    .span(class_("en").class_("copyright"))
                        .write("© " + copyright_en)
                    ._span()
                  ._footer();
            }
        };
    }
    
    private byte[] serialize(HtmlTitle title, Renderable tableHeader, Renderable tableBody, HttpServletRequest request) {
        HtmlCanvas html = HtmlCanvasFactory.createCanvas(request, null, new StringWriter(8192));
        
        try {
            html.html()
                .render(DocType.HTML5)
                .head()
                  .render(pageHead(title))
                ._head()
                .body(class_("fi"))
                  .render(pageHeader(title))
                  .section()
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
                  .script(type("text/javascript"))
                      .write("if (window.location.hash.indexOf('deep') == -1) { document.getElementById('loadButton').style.visibility = 'visible'; } else { loadAdditionalContent(); }", false)
                  ._script()
                ._body()
              ._html();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return html.toHtml().getBytes(Charset.forName("UTF-8"));
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
                    html.th()
                        .span(class_("fi").title(pseudo ? null : docDescription(member.getMember()).getOrElse(null)))
                            .write(member.getName())
                        ._span();
                    html.span(class_("en").title(pseudo ? null : docDescription_en(member.getMember()).getOrElse(null)))
                            .write(member.getName()).i().write(pseudo ? null : docName_en(member.getMember()).getOrElse(""))._i()
                        ._span();
                    html._th();
                }
            }
        };
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
                    html.td(class_(val == null ? "null" : ""))
                          .render(htmlModule.toRenderable(val))
                        ._td();
                }
            }
          };
    }
}
