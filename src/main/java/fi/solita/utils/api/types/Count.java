package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import fi.solita.utils.api.Documentation;

@Documentation(name_en = "Count", description = "Tulosjoukon maksimikoko. Oletuksena rajoittamaton", description_en = "Result set size. Unlimited by default")
public final class Count {
    public static final Count DEFAULT = new Count(Integer.MAX_VALUE);
    public static final List<Integer> validValues = newList(1,10,50,100,500,1000,4000);
    
    public final int value;
    
    public Count(int value) {
        this.value = value;
    }
}
