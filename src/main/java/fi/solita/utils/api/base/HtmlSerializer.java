package fi.solita.utils.api.base;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public abstract class HtmlSerializer<T> {
    public Renderable toRenderable(final HtmlModule htmlModule, final T value) {
        return new Renderable() {
            @Override
            public void renderOn(HtmlCanvas html) throws IOException {
                HtmlSerializer.this.renderOn(value, html, htmlModule);
            }
        };
    }
    
    public abstract void renderOn(T value, HtmlCanvas html, HtmlModule module) throws IOException;
}