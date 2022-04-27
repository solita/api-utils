package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.drop;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.Functional.zipWithIndex;
import static fi.solita.utils.functional.FunctionalA.max;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;
import static fi.solita.utils.functional.Transformers.append;
import static fi.solita.utils.functional.Transformers.prepend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;

import fi.solita.utils.api.base.excel.ExcelModule;
import fi.solita.utils.api.base.excel.ExcelSerializer.Cells;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil_;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple2;
import fi.solita.utils.meta.MetaNamedMember;

public class ExcelConversionService {

    private final ExcelModule module;

    public ExcelConversionService(ExcelModule module) {
        this.module = module;
    }
    
    public <T> byte[] serialize(HttpServletResponse res, String filename, T obj, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return serialize(res, filename, newList(obj), members);
    }
    
    public <T> byte[] serialize(HttpServletResponse res, String filename, T[] obj) {
        return serialize(res, filename, newList(obj));
    }
    
    public <T> byte[] serialize(HttpServletResponse res, String filename, final Iterable<T> obj) {
        return serialize(res, filename, newList(obj), newList(new MetaNamedMember<T, T>() {
            @Override
            public T apply(T t) {
                return t;
            }

            @Override
            public AccessibleObject getMember() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                return "";
            }
        }));
    }
    
    public <T> byte[] serialize(HttpServletResponse res, String filename, final Collection<T> obj, final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return serialize(res, filename, header(members), map(ExcelConversionService_.<T>regularBodyRow().ap(members), obj));
    }

    public <K,V> byte[] serialize(HttpServletResponse res, String filename, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(res, filename, header(members), map(ExcelConversionService_.<V>regularBodyRow().ap(members), flatten(obj.values())));
    }
    
    public <K,V> byte[] serializeSingle(HttpServletResponse res, String filename, final Map<K,V> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(res, filename, header(members), map(ExcelConversionService_.<V>regularBodyRow().ap(members), obj.values()));
    }
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithKey(HttpServletResponse res, String filename, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        // empty header if there's no simple key. This is a bit too hackish...
        headers = cons(new MetaNamedMember<V, Object>() {
            @Override
            public Object apply(V t) {
                throw new UnsupportedOperationException();
            }
            @Override
            public AccessibleObject getMember() {
                throw new UnsupportedOperationException();
            }
            @Override
            public String getName() {
                return "";
            }
        }, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(res, filename, header(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members));
    }
    
    @SuppressWarnings("unchecked")
    public <K,V> byte[] serializeWithKey(HttpServletResponse res, String filename, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members, final MetaNamedMember<? super V,?> key) {
        Iterable<? extends MetaNamedMember<V,Object>> headers = (Iterable<MetaNamedMember<V,Object>>)members;
        members = filter(not(equalTo((MetaNamedMember<V,Object>)key)), (Iterable<MetaNamedMember<V,Object>>)members);
        headers = cons((MetaNamedMember<V,Object>)key, (Iterable<MetaNamedMember<V,Object>>)members);
        return serialize(res, filename, header(headers), mapBody(obj, (Iterable<MetaNamedMember<V,Object>>)members));
    }
    
    private byte[] serialize(HttpServletResponse res, String filename, Iterable<String> tableHeader, Iterable<Iterable<Pair<Object,Class<Object>>>> tableBody) {
        XSSFWorkbook wb = new XSSFWorkbook();
        String safeName = WorkbookUtil.createSafeSheetName(filename);
        Sheet sheet = wb.createSheet(safeName);

        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setBorderBottom(BorderStyle.THIN);
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        CellStyle wrapStyle = wb.createCellStyle();
        wrapStyle.setWrapText(true);
        
        Row header = sheet.createRow((short)0);
        header.setRowStyle(headerStyle);
        for (Tuple2<Integer, Iterable<Pair<Object,Class<Object>>>> r: zipWithIndex(tableBody)) {
            Row row = sheet.createRow((short)(int)r._1 + 1);
            int column = 0;
            List<Cells> rowCells = newMutableList();
            for (Pair<Object,Class<Object>> c: r._2) {
                Cells cells = module.serialize(row, column, c._1, c._2);
                column += cells.cells.size();
                rowCells.add(cells);
            }
            for (Cell c: newList(drop(column, row))) {
                // poistetaan ylimääräiset, sillä sarjallistus on saattanut niitä luoda ennenkuin ne on typistetty yhdeksi, esim collectionien tapauksessa :(
                row.removeCell(c);
            }
            if (isEmpty(header)) {
                createHeader(tableHeader, rowCells, header, headerStyle);
            }
            
            int maxLines = 1;
            for (Cell cell: row) {
                if (cell.getCellType().equals(CellType.STRING)) {
                    cell.setCellStyle(wrapStyle);
                    maxLines = max(maxLines, cell.getStringCellValue().split("\r\n").length);
                }
            }
            row.setHeight((short)(row.getHeight() * maxLines));
            
            Assert.equal(size(header), size(row));
        }
        
        for (int c = 0; c < header.getLastCellNum(); ++c) {
            sheet.autoSizeColumn(c);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            wb.write(out);
            out.close();
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".xlsx");
        return out.toByteArray();
    }
    
    private void createHeader(Iterable<String> tableHeader, Iterable<Cells> row, Row header, CellStyle headerStyle) {
        Iterator<String> fieldNames = tableHeader.iterator();
        int column = 0;
        for (Cells cells: row) {
            CharSequence currentFieldName = fieldNames.next();
            String unit = cells.unit.map(prepend(" (").andThen(append(")"))).getOrElse("");
            if (cells.headers.isEmpty()) {
                // skip
            } else if (cells.headers.equals(newList(""))) {
                Cell headerCell = header.createCell(column);
                headerCell.setCellValue(currentFieldName + unit);
                headerCell.setCellStyle(headerStyle);
                column += 1;
                for (@SuppressWarnings("unused") Object o: tail(cells.cells)) {
                    column += 1;
                    header.createCell(column);
                }
            } else {
                Assert.equal(cells.cells.size(), cells.headers.size());
                for (String h: map(prepend(currentFieldName + " ").andThen(append(cells.headers.size() == 1 ? unit : "")), cells.headers)) {
                    Cell headerCell = header.createCell(column);
                    headerCell.setCellValue(h);
                    headerCell.setCellStyle(headerStyle);
                    column += 1;
                }
            }
        }
    }
    
    private static <K,V,O> Iterable<Iterable<Pair<Object,Class<Object>>>> mapBody(final Map<K, ? extends Iterable<V>> obj, final Iterable<? extends MetaNamedMember<V, O>> members) {
        return map(ExcelConversionService_.<K,V,O>mapBodyRow().ap(members), flatMap(ExcelConversionService_.<K,V>flatKeyToValues(), obj.entrySet()));
    }
    
    @SuppressWarnings("unchecked")
    static <K,V,O> Iterable<Pair<Object,Class<Object>>> mapBodyRow(Iterable<? extends MetaNamedMember<V, O>> members, K key, V value) {
        return cons(Pair.of((Object)key, (Class<Object>)key.getClass()), map(ExcelConversionService_.<V,O>foo().ap(value), members));
    }
    
    @SuppressWarnings("unchecked")
    static <V,O> Pair<Object,Class<Object>> foo(V value, MetaNamedMember<V, O> member) {
        return (Pair<Object,Class<Object>>)(Object)Pair.of(member.apply(value), MemberUtil.actualTypeUnwrappingOptionAndEither(member));
    }
    
    static <K,V> Iterable<Map.Entry<K,V>> flatKeyToValues(K key, Iterable<V> values) {
        return map(ExcelConversionService_.<K,V>makePair().ap(key), values);
    }
    
    static <K,V> Map.Entry<K,V> makePair(K key, V value) {
        return Pair.of(key, value);
    }
    
    static <T> Iterable<String> header(final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return map(MemberUtil_.memberName, members);
    }
    
    static <T> Iterable<Pair<Object,Class<Object>>> regularBodyRow(final Iterable<? extends MetaNamedMember<T, ?>> members, final T obj) {
        return map(ExcelConversionService_.<T>cell().ap(obj), members);
    }
    
    @SuppressWarnings("unchecked")
    static <T> Pair<Object,Class<Object>> cell(final T obj, final MetaNamedMember<T, ?> member) {
        if (member.getName().equals("")) {
            // oma dummy-otsikko
            return Pair.of((Object)member.apply(obj), (Class<Object>)(obj instanceof Option && ((Option<?>)obj).isDefined() ? ((Option<?>)obj).get().getClass() : obj.getClass()));
        } else {
            return Pair.of((Object)member.apply(obj), (Class<Object>)MemberUtil.actualTypeUnwrappingOptionAndEither(member));
        }
    }
}
