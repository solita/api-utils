package fi.solita.utils.api.html;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import org.rendersnake.HtmlCanvas;

import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Option;

public abstract class HttpServletCanvas<REQ> extends HtmlCanvas {
    
    protected REQ request;
    
    public HttpServletCanvas(REQ request, Writer out) {
        this.request = request;
        this.out = out;
    }

    @Override
    public HtmlCanvas createLocalCanvas(){
        throw new UnsupportedOperationException();
    }
    
    public abstract Request getRequest();
    
    public abstract String getContextPath();

    public abstract String getRequestPath();
    public abstract Option<String> getRequestQueryString();
    
    public static <REQ> HtmlCanvas of(REQ req, OutputStreamWriter ow) {
        try {
            return classImplements("javax.servlet.http.HttpServletRequest", req.getClass())
                    ? (HtmlCanvas) Class.forName("fi.solita.utils.api.html.JavaxHttpServletCanvas").getDeclaredConstructors()[0].newInstance(req, ow)
                    : classImplements("jakarta.servlet.http.HttpServletRequest", req.getClass())
                    ? (HtmlCanvas) Class.forName("fi.solita.utils.api.html.JakartaHttpServletCanvas").getDeclaredConstructors()[0].newInstance(req, ow)
                    : null;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final boolean classImplements(String interf, Class<?> source) {
        if (source.getName().equals(interf)) {
            return true;
        }
        for (Class<?> i: source.getInterfaces()) {
            if (i.getName().equals(interf)) {
                return true;
            }
            if (classImplements(interf, i)) {
                return true;
            }
        }
        for (Class<?> sup: Option.of(source.getSuperclass())) {
            if (classImplements(interf, sup)) {
                return true;
            }
        }
        return false;
    }
}
