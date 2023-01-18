package fi.solita.utils.api.html;

import static fi.solita.utils.functional.Option.Some;
import static org.rendersnake.HtmlAttributesFactory.for_;
import static org.rendersnake.HtmlAttributesFactory.href;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.lang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
                .dt(lang(lang.getOrElse(null)))
                    .write(dt)
                ._dt()
                .dd(lang(lang.getOrElse(null)))
                    .render(dd)
                ._dd();
            }
        };
    }
    
    public static Renderable langSelectorInput = new Renderable() {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html.input(id("lang-selector").type("checkbox"));
        }
    };
    
    public static Renderable langSelectorLabel = new Renderable() {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html.label(for_("lang-selector").title("Switch between Finnish/English"))
                    .write("Finnish 🇫🇮 / English 🇬🇧")
                ._label();
        }
    };
    
    public static final String langSelectorCSS = ""
        + "#lang-selector                                   { display: none; }"
        + "label[for='lang-selector']                       { margin-left: 1em; }"
        + "[lang]:not(:lang(fi)),"
        + "#lang-selector:checked ~ * :lang(fi)             { display: none !important; }"
        + "#lang-selector:checked ~ * [lang]:not(:lang(fi)) { display: inherit !important; }";
    
    public static final String calculateHash(final String content) {
        try {
            return "sha256-" +  new String(Base64.getEncoder().encode(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
