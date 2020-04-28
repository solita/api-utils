package fi.solita.utils.api.common;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.util.RequestUtil.ETags;

public class RevisionedRequestData extends RequestData {
    public final Revision revision;
    
    public RevisionedRequestData(SerializationFormat format, ETags etags, Revision revision) {
        super(format, etags);
        this.revision = revision;
    }
}
