package fi.solita.utils.api.base.tsv;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import fi.solita.utils.api.util.Assert;
import java.util.Optional;

public abstract class TsvSerializer<T> {
    public static final class Cells extends fi.solita.utils.api.base.Cells<CharSequence> {
        public Cells(CharSequence cell) {
            super(cell);
        }
        
        public Cells(CharSequence cell, String stringRepresentation) {
            super(cell, stringRepresentation);
        }
        
        @SuppressWarnings("unchecked")
        public Cells(Iterable<? extends CharSequence> cells, String stringRepresentation) {
            super((Iterable<CharSequence>) cells, stringRepresentation);
        }
        
        private Cells(List<CharSequence> cells, Optional<CharSequence> stringRepresentation, Optional<String> unit, List<String> headers) {
            super(cells, stringRepresentation, unit, headers);
        }
        
        public final Cells withUnit(Optional<String> unit) {
            return new Cells(cells, stringRepresentation, unit, headers);
        }

        public final Cells withHeaders(List<String> headers) {
            Assert.equal(cells.size(), headers.size());
            return new Cells(cells, stringRepresentation, unit, headers);
        }
    }

    public abstract Cells render(TsvModule module, T value);
    
    /**
     * @param module  
     * @param type 
     */
    public List<String> columns(TsvModule module, Class<T> type) {
        return newList("");
    }
    
    public TsvSerializer<T> withUnit(final Optional<String> unit) {
        final TsvSerializer<T> self = this;
        return new TsvSerializer<T>() {
            @Override
            public Cells render(TsvModule module, T value) {
                return self.render(module, value).withUnit(unit);
            }
        };
    }
    
    public TsvSerializer<T> withHeaders(final List<String> headers) {
        final TsvSerializer<T> self = this;
        return new TsvSerializer<T>() {
            @Override
            public Cells render(TsvModule module, T value) {
                return self.render(module, value).withHeaders(headers);
            }
        };
    }
}