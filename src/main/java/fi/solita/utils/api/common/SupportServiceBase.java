package fi.solita.utils.api.common;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Option.Some;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.solita.utils.api.RequestUtil;
import fi.solita.utils.api.ResponseUtil;
import fi.solita.utils.api.SwaggerSupport;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class SupportServiceBase {

    public void redirectToCurrentTime(HttpServletRequest req, HttpServletResponse res) {
        ResponseUtil.redirect307(RequestUtil.getContextRelativePath(req), req, res, newMap(Pair.of("time", SwaggerSupport.intervalNow())));
    }

    public Option<RequestData> resolveFormat(HttpServletRequest request, HttpServletResponse response) {
        SerializationFormat format = RequestUtil.resolveFormat(request);
        response.setContentType(format.mediaType);
        return Some(new RequestData(format, RequestUtil.getETags(request)));
    }

    public RequestData checkUrlAndResolveFormat(HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        checkUrl(request, acceptedParams);
        return resolveFormat(request, response).get();
    }
    
    public void checkUrl(HttpServletRequest request, String... acceptedParams) {
        RequestUtil.checkURL(request, newArray(String.class, cons("time", cons("profile", cons("srsName", acceptedParams)))));
    }
}
