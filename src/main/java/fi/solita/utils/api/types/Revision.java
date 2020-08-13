package fi.solita.utils.api.types;

public class Revision {

    public final long revision;
    
    public Revision(long revision) {
        this.revision = revision;
    }
    
    @Override
    public String toString() {
        return Long.toString(revision);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (revision ^ (revision >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Revision other = (Revision) obj;
        if (revision != other.revision)
            return false;
        return true;
    }
}
