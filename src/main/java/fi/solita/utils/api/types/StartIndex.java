package fi.solita.utils.api.types;

import fi.solita.utils.api.Assert;
import fi.solita.utils.api.Documentation;

@Documentation(name_en = "StartIndex", description = "Tulosjoukon alkuindeksi, oletuksena 1", description_en = "Result set start index. 1 by default")
public final class StartIndex {
    public static final StartIndex DEFAULT = new StartIndex(1);
    
    public final int value;
    
    public StartIndex(int value) {
        this.value = Assert.positive(value);
    }
}
