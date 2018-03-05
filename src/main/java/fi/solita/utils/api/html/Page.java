package fi.solita.utils.api.html;

import static org.rendersnake.HtmlAttributesFactory.class_;

import java.io.IOException;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.RenderableWrapper;

public class Page extends RenderableWrapper {

    private final String title;
    private final String copyright_fi;
    private final String copyright_en;

    public Page(String title, String copyright_fi, String copyright_en, Renderable component) {
        super(component);
        this.title = title;
        this.copyright_fi = copyright_fi;
        this.copyright_en = copyright_en;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.html()
            .render(DocType.HTML5)
            .head()
              .title().write(title)
              ._title()
              .style().write(
                      "html    { font-family: sans-serif; font-weight: lighter; }"
                    + "h1      { font-weight: lighter; }"
                    + "dt      { font-weight: bolder; margin-top: 1em; }"
                    + "section { border-radius: 5px; border: 1px solid #ddd; overflow: auto; margin: 0.25em; padding: 0 0.5em; }"
                    + "footer  { padding-top: 10px; color: #ccc; font-style: italic; font-size: 0.8em; clear: left; }"
                    , false)
            ._style()
          ._head()
          .body()
              .header()
                  .h1()
                      .write(title)
                  ._h1()
              ._header()
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
