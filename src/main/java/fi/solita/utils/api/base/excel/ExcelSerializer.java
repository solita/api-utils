package fi.solita.utils.api.base.excel;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Option;

public abstract class ExcelSerializer<T> {
    public static final class Cells extends fi.solita.utils.api.base.Cells<Cell> {
        public Cells(Cell cell) {
            super(cell);
        }
        
        public Cells(Cell cell, CharSequence stringRepresentation) {
            super(cell, stringRepresentation);
        }
        
        public Cells(Iterable<Cell> cells, CharSequence stringRepresentation) {
            super(cells, stringRepresentation);
        }
        
        private Cells(List<Cell> cells, Option<CharSequence> stringRepresentation, Option<String> unit, List<String> headers) {
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
    
    public abstract Cells render(ExcelModule module, Row row, int columnIndex, T value);
    
    public List<String> columns(ExcelModule module, Class<T> type) {
        return newList("");
    }
    
    public ExcelSerializer<T> withUnit(final Option<String> unit) {
        final ExcelSerializer<T> self = this;
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return self.render(module, row, columnIndex, value).withUnit(unit);
            }
        };
    }
    
    public ExcelSerializer<T> withHeaders(final List<String> headers) {
        final ExcelSerializer<T> self = this;
        return new ExcelSerializer<T>() {
            @Override
            public Cells render(ExcelModule module, Row row, int columnIndex, T value) {
                return self.render(module, row, columnIndex, value).withHeaders(headers);
            }
        };
    }
}