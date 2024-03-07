package fi.solita.utils.api.html;

import static fi.solita.utils.functional.FunctionalC.drop;

import java.io.Writer;

import fi.solita.utils.api.util.ServletRequestUtil;
import fi.solita.utils.functional.Option;
import jakarta.servlet.http.HttpServletRequest;

public class JakartaHttpServletCanvas extends HttpServletCanvas<HttpServletRequest> {
    
    public JakartaHttpServletCanvas(HttpServletRequest request, Writer out) {
        super(request, out);
    }

    public String getContextPath() {
        return ServletRequestUtil.getContextPath((HttpServletRequest) request);
    }

    public String getRequestPath() {
        String apiVersionBasePath = ServletRequestUtil.getApiVersionBasePath((HttpServletRequest) request);
        String apiVersionRelativePathWithoutRevision = ServletRequestUtil.getAPIVersionRelativePathWithoutRevision((HttpServletRequest) request);
        return apiVersionBasePath + drop(1, apiVersionRelativePathWithoutRevision);
    }

    public Option<String> getRequestQueryString() {
        return Option.of(((HttpServletRequest) request).getQueryString());
    };
}
