package fi.solita.utils.api.html;

import static fi.solita.utils.functional.FunctionalC.drop;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Option;

public class JavaxHttpServletCanvas extends HttpServletCanvas<HttpServletRequest> {
    
    public JavaxHttpServletCanvas(HttpServletRequest request, Writer out) {
        super(request, out);
    }

    public String getContextPath() {
        return RequestUtil.getContextPath(request.getContextPath(), Option.of(request.getHeader("X-Forwarded-Prefix")));
    }

    public String getRequestPath() {
        String contextPath = RequestUtil.getContextPath(request.getContextPath(), Option.of(request.getHeader("X-Forwarded-Prefix")));
        String contextRelativePath = RequestUtil.getContextRelativePath(request.getServletPath(), Option.of(request.getPathInfo()));
        
        String apiVersionBasePath = RequestUtil.getApiVersionBasePath(contextPath, contextRelativePath);
        String apiVersionRelativePath = RequestUtil.getAPIVersionRelativePath(contextRelativePath);
        String apiVersionRelativePathWithoutRevision = RequestUtil.getAPIVersionRelativePathWithoutRevision(apiVersionRelativePath);
        return apiVersionBasePath + drop(1, apiVersionRelativePathWithoutRevision);
    }

    public Option<String> getRequestQueryString() {
        return Option.of(((HttpServletRequest) request).getQueryString());
    };
}