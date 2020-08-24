package fi.solita.utils.api.html;

import static fi.solita.utils.functional.Option.Some;
import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fi.solita.utils.functional.Option;

public abstract class UI {
    
    public static Renderable concat(final Renderable a, final Renderable b) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html.render(a).render(b);
            }
        };
    }

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
    
    public static Renderable definition_fi(final String dt, final String dd) throws IOException {
        return definition_fi(dt, text(dd));
    }
    
    public static Renderable definition_fi(final String dt, final Renderable dd) throws IOException {
        return definition(Some("fi"), dt, dd);
    }
    
    public static Renderable definition_en(final String dt, final String dd) throws IOException {
        return definition_en(dt, text(dd));
    }
    
    public static Renderable definition_en(final String dt, final Renderable dd) throws IOException {
        return definition(Some("en"), dt, dd);
    }
    
    public static Renderable definition(final Option<String> lang, final String dt, final String dd) throws IOException {
        return definition(lang, dt, text(dd));
    }
    
    public static Renderable definition(final Option<String> lang, final String dt, final Renderable dd) throws IOException {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                html
                .dt(class_(lang.getOrElse(null)))
                    .write(dt)
                ._dt()
                .dd(class_(lang.getOrElse(null)))
                    .render(dd)
                ._dd();
            }
        };
    }
}
