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
        Count other = (Count) obj;
        if (value != other.value)
            return false;
        return true;
    }
    
    
}
