package fi.solita.utils.api.html;

import static org.rendersnake.HtmlAttributesFactory.class_;

import java.io.IOException;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.RenderableWrapper;

import fi.solita.utils.api.format.HtmlConversionService;
import fi.solita.utils.api.format.HtmlConversionService.HtmlTitle;

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
              .title().write(title_fi)._title()
              .style().write(
                      "html     { font-family: sans-serif; font-weight: lighter; }"
                    + ".lang li { display: inline; padding: 0 1em; border-width: 0 0 0 1px; border-style: dotted; cursor: pointer; }"
                    + ".lang li:first-child { border: none; }"
                    + "body.en .fi, body.fi .en { display: none !important; }"
                    + "header   { display: flex; }"
                    + "h1       { font-weight: lighter; flex: 1; }"
                    + "dt       { font-weight: bolder; margin-top: 1em; }"
                    + "section  { border-radius: 5px; border: 1px solid #ddd; overflow: auto; margin: 0.25em; padding: 0 0.5em; }"
                    + "footer   { padding-top: 10px; color: #ccc; font-style: italic; font-size: 0.8em; clear: left; }"
                    , false)
            ._style()
          ._head()
          .body(class_("fi"))
              .render(HtmlConversionService.pageHeader(new HtmlTitle("") {
                @Override
                public void renderOn(HtmlCanvas html) throws IOException {
                    html.span(class_("fi")).write(title_fi)._span()
                        .span(class_("en")).write(title_en)._span();
                }
            }))
              .render(component)
              .footer()
                  .span(class_("fi").class_("copyright"))
                      .write("© " + copyright_fi)
                  ._span()
                  .span(class_("en").class_("copyright"))
                      .write("© " + copyright_en)
                  ._span()
              ._footer()
          ._body()
        ._html();
    }

}
