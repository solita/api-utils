package fi.solita.utils.api.request;

import java.util.Set;

import fi.solita.utils.api.types.Revision;

public interface RevisionProvider {
    public Set<Revision> getValidRevisions();
    public boolean withinTolerance(Revision revision1, Revision revision2);
}
