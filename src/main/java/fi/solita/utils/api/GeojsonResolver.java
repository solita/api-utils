package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;

import java.util.Collection;

import fi.solita.utils.api.format.geojson.FeatureObject;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class GeojsonResolver {

    public abstract Collection<FeatureObject> getFeaturesForResolvable(MetaNamedMember<?,Object> member, Object resolvable);
    
    @SuppressWarnings("unchecked")
    public <DTO> Collection<FeatureObject> getResolvedFeatures(Iterable<DTO> d, Includes<DTO> includes) {
        Iterable<FeatureObject> ret = emptyList();
        for (final MetaNamedMember<DTO,Object> member: (Iterable<MetaNamedMember<DTO,Object>>)(Object)filter(ResolvableMemberProvider_.isResolvableMember, includes)) {
            for (DTO t: d) {
                ret = concat(ret, flatMap(new Apply<Object, Collection<FeatureObject>>() {
                    @Override
                    public Collection<FeatureObject> apply(Object resolvable) {
                        return getFeaturesForResolvable(member, resolvable);
                    }
                }, ResolvableMemberProvider.unwrapResolvable(((ResolvableMember<DTO>)member).original, t)));
            }
        }
        return newList(ret);
    }
}
