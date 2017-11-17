package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Option.Some;

import java.util.List;

import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Option;

public class Cells<T> {
    public final List<T> cells;
    public final Option<CharSequence> stringRepresentation;
    public final Option<String> unit;
    public final List<String> headers;
    
    public Cells(T cell) {
        this(newList(cell), Option.<CharSequence>None(), Option.<String>None(), Collections.<String>emptyList());
    }
    
    public Cells(T cell, CharSequence stringRepresentation) {
        this(newList(cell), Some(stringRepresentation), Option.<String>None(), Collections.<String>emptyList());
    }
    
    public Cells(Iterable<T> cells, CharSequence stringRepresentation) {
        this(newList(cells), Some(stringRepresentation), Option.<String>None(), Collections.<String>emptyList());
    }
    
    protected Cells(List<T> cells, Option<CharSequence> stringRepresentation, Option<String> unit, List<String> headers) {
        this.cells = cells;
        this.stringRepresentation = stringRepresentation;
        this.unit = unit;
        this.headers = headers;
    }
}