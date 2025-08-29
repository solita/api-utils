package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.math.BigDecimal;
import java.util.List;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;

import fi.solita.utils.api.filtering.Filter;
import fi.solita.utils.api.filtering.FilterType;
import fi.solita.utils.api.filtering.Filter_;
import fi.solita.utils.api.filtering.Literal;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyBi;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.lens.Setter;

public class Transform {
    public static <BOUNDS> BOUNDS resolveBounds(BOUNDS bbox, Filters filters, Apply<String,BOUNDS> fromWKT) {
        if (filters != null) {
            List<List<Filter>> allSpatialFilters = filters.spatialFilters();
            List<Filter> spatialFilters = newList(flatten(allSpatialFilters));
            Assert.lessThanOrEqual(spatialFilters.size(), 1);                // multiple spatial constraints not supported.
            Assert.True(spatialFilters.isEmpty() || filters.or.size() <= 1); // OR queries with spatial constraints not supported.
            for (Filter intersect: filter(Filter_.pattern.andThen(equalTo(FilterType.INTERSECTS)), spatialFilters)) {
                Assert.Null(bbox, "BBOX cannot be given together with spatial filtering");
                Literal wkt = Assert.singleton(intersect.literals);
                return fromWKT.apply(wkt.getValue().left.get());
            }
        }
        return bbox;
    }
    
    private static MathTransform findMathTransform(SRSName sourceCRS, SRSName targetCRS) {
        try {
            // For some reason geotools seems to not recognize this, so let's utilize 4326
            if (sourceCRS.equals(SRSName.CRS84)) {
                return CRS.findMathTransform(CRS.decode(SRSName.EPSG4326.longValue), CRS.decode(targetCRS.longValue));
            } else if (targetCRS.equals(SRSName.CRS84)) {
                return CRS.findMathTransform(CRS.decode(sourceCRS.longValue), CRS.decode(SRSName.EPSG4326.longValue));
            } else {
                return CRS.findMathTransform(CRS.decode(sourceCRS.longValue), CRS.decode(targetCRS.longValue));
            }
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Pair<BigDecimal,BigDecimal> transformCoordinate(SRSName sourceCRS, SRSName targetCRS, Pair<BigDecimal,BigDecimal> coord) {
        try {
            double[] from = {coord.left().doubleValue(), coord.right().doubleValue()};
            double[] ret = transformCoordinate(sourceCRS, targetCRS, from);
            return Pair.of(BigDecimal.valueOf(ret[0]), BigDecimal.valueOf(ret[1]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static double[] transformCoordinate(SRSName sourceCRS, SRSName targetCRS, double[] coord) {
        final MathTransform transformer = findMathTransform(sourceCRS, targetCRS);
        try {
            double[] from = coord;
            if (sourceCRS.equals(SRSName.CRS84)) {
                from = new double[] {from[1], from[0]};
            }
            double[] to = transformer.transform(new Position2D(from[0], from[1]), null).getCoordinate();
            if (targetCRS.equals(SRSName.CRS84)) {
                to = new double[] { to[1], to[0] };
            }
            double[] ret = coord.clone();
            ret[0] = to[0];
            ret[1] = to[1];
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final <T,G> Function1<T,T> transforming(final SRSName source, final SRSName target, final Setter<T, G> setter, final ApplyBi<SRSName,G,G> f) {
        return source.equals(target) ? Function.<T>id() : new Function1<T, T>() {
            @Override
            public T apply(T t) {
                return t == null ? t : setter.modify(t, Function.of(f).ap(target));
            }
        };
    }
}
