package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.newList;


import java.util.List;

import fi.solita.utils.functional.Collections;
import java.util.Optional;

public class Cells<T> {
    public final List<T> cells;
    public final Optional<CharSequence> stringRepresentation;
    public final Optional<String> unit;
    public final List<String> headers;
    
    public Cells(T cell) {
        this(newList(cell), Optional.empty(), Optional.empty(), Collections.<String>emptyList());
    }
    
    public Cells(T cell, CharSequence stringRepresentation) {
        this(newList(cell), Optional.of(stringRepresentation), Optional.empty(), Collections.<String>emptyList());
    }
    
    public Cells(Iterable<T> cells, CharSequence stringRepresentation) {
        this(newList(cells), Optional.of(stringRepresentation), Optional.empty(), Collections.<String>emptyList());
    }
    
    protected Cells(List<T> cells, Optional<CharSequence> stringRepresentation, Optional<String> unit, List<String> headers) {
        this.cells = cells;
        this.stringRepresentation = stringRepresentation;
        this.unit = unit;
        this.headers = headers;
    }
}