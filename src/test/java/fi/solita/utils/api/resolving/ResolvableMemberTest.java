package fi.solita.utils.api.resolving;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.junit.Test;

import fi.solita.utils.api.resolving.ResolvableMemberProvider.Type;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.RedundantPropertiesException;
import fi.solita.utils.functional.Collections;

public class ResolvableMemberTest {
    public String dummy;
    
    public static final ResolvableMember<ResolvableMemberTest> empty = new ResolvableMember<ResolvableMemberTest>(ResolvableMemberTest_.dummy, Collections.<PropertyName>emptySortedSet(), Type.ExternalKnown);
    public static final ResolvableMember<ResolvableMemberTest> someData = new ResolvableMember<ResolvableMemberTest>(ResolvableMemberTest_.dummy, newSortedSet(Comparator.naturalOrder(), newList(PropertyName.of("dummy"))), Type.ExternalKnown);
    public static final ResolvableMember<ResolvableMemberTest> allData = new ResolvableMember<ResolvableMemberTest>(ResolvableMemberTest_.dummy, newSortedSet(Comparator.naturalOrder(), newList(PropertyName.of(""))), Type.ExternalKnown);
    
    @Test
    public void combine_emptyIsIdentity() {
        assertEquals(someData, empty.combine(someData));
        assertEquals(someData, someData.combine(empty));
        assertEquals(allData, empty.combine(allData));
        assertEquals(allData, allData.combine(empty));
    }
    
    @Test
    public void combine_allDataIsIdempotent() {
        assertEquals(allData, allData.combine(allData));
    }
    
    @Test(expected = RedundantPropertiesException.class)
    public void combine_allDataFailsForNonEmpty1() {
        someData.combine(allData);
    }
    
    @Test(expected = RedundantPropertiesException.class)
    public void combine_allDataFailsForNonEmpty2() {
        allData.combine(someData);
    }
}
