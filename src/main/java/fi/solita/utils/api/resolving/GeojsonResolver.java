package fi.solita.utils.api.resolving;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;

import java.util.Collection;
import java.util.List;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.format.geojson.FeatureObject;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class GeojsonResolver {

    public abstract Collection<FeatureObject> getFeaturesForResolvable(MetaNamedMember<?,Object> member, Object resolvable);
    
    @SuppressWarnings("unchecked")
    public <DTO> Collection<FeatureObject> getResolvedFeatures(Iterable<DTO> d, Includes<DTO> includes) {
        List<FeatureObject> ret = newMutableList();
        for (final MetaNamedMember<DTO,Object> member: (Iterable<MetaNamedMember<DTO,Object>>)(Object)filter(ResolvableMemberProvider_.isResolvableMember, includes)) {
            for (DTO t: d) {
                ret.addAll(newList(flatMap(new Apply<Object, Collection<FeatureObject>>() {
                    @Override
                    public Collection<FeatureObject> apply(Object resolvable) {
                        return getFeaturesForResolvable(member, resolvable);
                    }
                }, ResolvableMemberProvider.unwrapResolvable(((ResolvableMember<DTO>)member).original, t))));
            }
        }
        return newList(ret);
    }
}
