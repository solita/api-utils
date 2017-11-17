package fi.solita.utils.api.html;

import static org.rendersnake.HtmlAttributesFactory.href;

import java.io.IOException;

import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public abstract class UI {

    public static Renderable text(final String text) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.write(text);
            }
        };
    }
    
    public static Renderable link(final HtmlAttributes attrs, final Renderable text) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.a(attrs).render(text)._a();
            }
        };
    }
    
    public static Renderable link(final String href) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.a(href(href)).write(href)._a();
            }
        };
    }
    
    public static Renderable definition(final String dt, final String dd) throws IOException {
        return definition(dt, text(dd));
    }
    
    public static Renderable definition(final String dt, final Renderable dd) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                .dt()
                    .write(dt)
                ._dt()
                .dd()
                    .render(dd)
                ._dd();
            }
        };
    }
}
