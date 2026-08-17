package fi.solita.utils.api.html;

import static fi.solita.utils.functional.FunctionalC.drop;

import java.io.Writer;

import fi.solita.utils.api.util.Headers;
import fi.solita.utils.api.util.JakartaRequest;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;

public class JakartaHttpServletCanvas extends HttpServletCanvas<HttpServletRequest> {
    
    public JakartaHttpServletCanvas(HttpServletRequest request, Writer out) {
        super(request, out);
    }
    
    @Override
    public Request getRequest() {
        return JakartaRequest.of(request);
    }

    public String getContextPath() {
        return RequestUtil.getContextPath(request.getContextPath(), Optional.ofNullable(request.getHeader(Headers.X_FORWARDED_PREFIX)));
    }

    public String getRequestPath() {
        String contextPath = RequestUtil.getContextPath(request.getContextPath(), Optional.ofNullable(request.getHeader(Headers.X_FORWARDED_PREFIX)));
        String contextRelativePath = RequestUtil.getContextRelativePath(request.getServletPath(), Optional.ofNullable(request.getPathInfo()));
        
        String apiVersionBasePath = RequestUtil.getApiVersionBasePath(contextPath, contextRelativePath);
        String apiVersionRelativePath = RequestUtil.getAPIVersionRelativePath(contextRelativePath);
        String apiVersionRelativePathWithoutRevision = RequestUtil.getAPIVersionRelativePathWithoutRevision(apiVersionRelativePath);
        return apiVersionBasePath + drop(1, apiVersionRelativePathWithoutRevision);
    }

    public Optional<String> getRequestQueryString() {
        return Optional.ofNullable(((HttpServletRequest) request).getQueryString());
    }
}
