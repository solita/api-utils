package fi.solita.utils.api.html;

import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.RenderableWrapper;

import static fi.solita.utils.functional.Option.None;
import fi.solita.utils.api.format.HtmlConversionService;
import fi.solita.utils.api.format.HtmlConversionService.HtmlTitle;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Pair;

public class Page extends RenderableWrapper {

    private final String title_fi;
    private final String title_en;
    private final String copyright_fi;
    private final String copyright_en;

    public Page(String title_fi, String title_en, String copyright_fi, String copyright_en, Renderable component) {
        super(component);
        this.title_fi = title_fi;
        this.title_en = title_en;
        this.copyright_fi = copyright_fi;
        this.copyright_en = copyright_en;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.html()
            .render(DocType.HTML5)
            .head()
              .meta(http_equiv("Content-Type").content("text/html;charset=UTF-8"))
              .meta(http_equiv("Content-Security-Policy").content("default-src 'self';style-src 'self' '" + UI.calculateHash(styles()) + "'"))
              .title().write(title_fi)._title()
              .style().write(styles(), false)
            ._style()
          ._head()
          .body()
              .render(HtmlConversionService.pageHeader(new HtmlTitle("") {
                @Override
                public void renderOn(HtmlCanvas html) throws IOException {
                    html.span(lang("fi")).write(title_fi)._span()
                        .span(lang("en")).write(title_en)._span();
                }
            }, ((HttpServletCanvas<?>)html).getRequest(), false, None(), Pair.of(new Renderable() {
                @Override
                public void renderOn(HtmlCanvas html) throws IOException {
                    // no-op
                }
            }, Collections.<String>emptySet())))
              .render(component)
              .footer()
                  .span(lang("fi").class_("copyright"))
                      .write("© " + copyright_fi)
                  ._span()
                  .span(lang("en").class_("copyright"))
                      .write("© " + copyright_en)
                  ._span()
              ._footer()
          ._body()
        ._html();
    }

    protected String styles() {
        return
          "html     { font-family: sans-serif; font-weight: lighter; }"
        + UI.langSelectorCSS
        + "header   { display: flex; }"
        + "h1       { font-weight: lighter; flex: 1; }"
        + "dt       { font-weight: bolder; margin-top: 1em; }"
        + "section  { border-radius: 5px; border: 1px solid #ddd; overflow: auto; margin: 0.25em; padding: 0 0.5em; }"
        + "footer   { padding-top: 10px; color: #ccc; font-style: italic; font-size: 0.8em; clear: left; }";
    }

}
