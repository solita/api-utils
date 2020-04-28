package fi.solita.utils.api.types;

import fi.solita.utils.api.Documentation;
import fi.solita.utils.api.util.Assert;

@Documentation(name_en = "StartIndex", description = "Tulosjoukon alkuindeksi, oletuksena 1", description_en = "Result set start index. 1 by default")
public final class StartIndex {
    public static final StartIndex DEFAULT = new StartIndex(1);
    
    public final int value;
    
    public StartIndex(int value) {
        this.value = Assert.positive(value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
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
        StartIndex other = (StartIndex) obj;
        if (value != other.value)
            return false;
        return true;
    }
    
    
}
