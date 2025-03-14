package fi.solita.utils.api.base.csv;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Option;

public abstract class CsvSerializer<T> {
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
        
        private Cells(List<CharSequence> cells, Option<CharSequence> stringRepresentation, Option<String> unit, List<String> headers) {
            super(cells, stringRepresentation, unit, headers);
        }
        
        public final Cells withUnit(Option<String> unit) {
            return new Cells(cells, stringRepresentation, unit, headers);
        }

        public final Cells withHeaders(List<String> headers) {
            Assert.equal(cells.size(), headers.size());
            return new Cells(cells, stringRepresentation, unit, headers);
        }
    }

    public abstract Cells render(CsvModule module, T value);
    
    /**
     * @param module  
     * @param type 
     */
    public List<String> columns(CsvModule module, Class<T> type) {
        return newList("");
    }
    
    public CsvSerializer<T> withUnit(final Option<String> unit) {
        final CsvSerializer<T> self = this;
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return self.render(module, value).withUnit(unit);
            }
        };
    }
    
    public CsvSerializer<T> withHeaders(final List<String> headers) {
        final CsvSerializer<T> self = this;
        return new CsvSerializer<T>() {
            @Override
            public Cells render(CsvModule module, T value) {
                return self.render(module, value).withHeaders(headers);
            }
        };
    }
}