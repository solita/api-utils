package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.math.BigDecimal;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.Filters.Filter;
import fi.solita.utils.api.types.Filters_.Filter_;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Function2;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.lens.Setter;

public class Transform {
    public static <BOUNDS> BOUNDS resolveBounds(BOUNDS bbox, Filters filters, Apply<String,BOUNDS> fromWKT) {
        if (filters != null) {
            List<Filter> spatialFilters = filters.spatialFilters();
            Assert.lessThanOrEqual(spatialFilters.size(), 1);
            for (Filter intersect: filter(Filter_.pattern.andThen(equalTo(Filters.INTERSECTS)), spatialFilters)) {
                Assert.Null(bbox, "BBOX cannot be given together with spatial filtering");
                String wkt = Assert.singleton(intersect.values);
                return fromWKT.apply(wkt);
            }
        }
        return bbox;
    }
    
    public static Pair<BigDecimal,BigDecimal> transformCoordinate(SRSName sourceCRS, SRSName targetCRS, Pair<BigDecimal,BigDecimal> coord) {
        final MathTransform transformer;
        try {
            // For some reason geotools seems to not recognize this, so let's utilize 4326
            if (sourceCRS.equals(SRSName.CRS84)) {
                transformer = CRS.findMathTransform(CRS.decode(SRSName.EPSG4326.longValue), CRS.decode(targetCRS.longValue));
            } else if (targetCRS.equals(SRSName.CRS84)) {
                transformer = CRS.findMathTransform(CRS.decode(sourceCRS.longValue), CRS.decode(SRSName.EPSG4326.longValue));
            } else {
                transformer = CRS.findMathTransform(CRS.decode(sourceCRS.longValue), CRS.decode(targetCRS.longValue));
            }
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
        try {
            double[] from = {coord.left.doubleValue(), coord.right.doubleValue()};
            if (sourceCRS.equals(SRSName.CRS84)) {
                from = new double[] {coord.right.doubleValue(), coord.left.doubleValue()};
            }
            Double[] ret = newArray(transformer.transform(new DirectPosition2D(from[0], from[1]), null).getCoordinate());
            if (targetCRS.equals(SRSName.CRS84)) {
                ret = new Double[] { ret[1], ret[0] };
            }
            return Pair.of(BigDecimal.valueOf(ret[0]), BigDecimal.valueOf(ret[1]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final <T,G> Function1<T,T> transforming(final SRSName target, final Setter<T, G> setter, final Function2<SRSName,G,G> f) {
        return new Function1<T, T>() {
            @Override
            public T apply(T t) {
                return t == null ? t : setter.modify(t, f.ap(target));
            }
        };
    }
}
