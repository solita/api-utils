package fi.solita.utils.api.resolving;

import org.joda.time.Interval;

import fi.solita.utils.meta.MetaField;

public interface ResolvingInterval<T> {
    public MetaField<T, Interval> resolvingInterval();
}
