package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;

import java.util.Collection;
import java.util.SortedSet;

import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;

public class RedundantPropertiesException extends RuntimeException {
    public final Collection<PropertyName> propertyNames;

    public RedundantPropertiesException(SortedSet<PropertyName> propertyNames) {
        super(mkString(",", map(PropertyName_.toString, propertyNames)));
        this.propertyNames = propertyNames;
    }
}