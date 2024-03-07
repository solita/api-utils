package fi.solita.utils.api.html;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import org.rendersnake.HtmlCanvas;

import fi.solita.utils.functional.Option;

public abstract class HttpServletCanvas<REQ> extends HtmlCanvas {
    
    @SuppressWarnings("unchecked")
    public static <REQ> REQ requestFor(HtmlCanvas canvas) {
        return (REQ) ((HttpServletCanvas<?>)canvas).request;
    }

    protected REQ request;
    
    public HttpServletCanvas(REQ request, Writer out) {
        this.request = request;
        this.out = out;
    }

    @Override
    public HtmlCanvas createLocalCanvas(){
        throw new UnsupportedOperationException();
    }
    
    public abstract String getContextPath();

    public abstract String getRequestPath();
    public abstract Option<String> getRequestQueryString();

    public static <REQ> HtmlCanvas of(REQ req, OutputStreamWriter ow) {
        try {
            return req.getClass().getName().equals("javax.servlet.http.HttpServletRequest")
                    ? (HtmlCanvas) Class.forName("fi.solita.utils.api.html.JavaxHttpServletCanvas").getDeclaredConstructors()[0].newInstance(req, ow)
                    : (HtmlCanvas) Class.forName("fi.solita.utils.api.html.JakartaHttpServletCanvas").getDeclaredConstructors()[0].newInstance(req, ow);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
