package fi.solita.utils.api.request;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil.ETags;

public class RequestData {
    public final SerializationFormat format;
    public final ETags etags;
    
    public RequestData(SerializationFormat format, ETags etags) {
        this.format = format;
        this.etags = etags;
    }
}
