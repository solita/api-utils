package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.tail;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;
import static fi.solita.utils.functional.Transformers.append;
import static fi.solita.utils.functional.Transformers.prepend;

import java.lang.reflect.AccessibleObject;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import fi.solita.utils.api.Assert;
import fi.solita.utils.api.MemberUtil;
import fi.solita.utils.api.MemberUtil_;
import fi.solita.utils.api.base.Cells_;
import fi.solita.utils.api.base.CsvModule;
import fi.solita.utils.api.base.CsvModule_;
import fi.solita.utils.api.base.CsvSerializer.Cells;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.meta.MetaNamedMember;

public class CsvConversionService {

    private final CsvModule module;

    public CsvConversionService(CsvModule module) {
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
        return serialize(res, filename, header(members), map(CsvConversionService_.<T>regularBodyRow().ap(this, members), obj));
    }
    
    public <K,V> byte[] serialize(HttpServletResponse res, String filename, final Map<K,? extends Iterable<V>> obj, Iterable<? extends MetaNamedMember<V, ?>> members) {
        return serialize(res, filename, header(members), map(CsvConversionService_.<V>regularBodyRow().ap(this, members), flatten(obj.values())));
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
    
    private byte[] serialize(HttpServletResponse res, String filename, Iterable<String> tableHeader, Iterable<Iterable<Cells>> tableBody) {
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".csv");
        
        List<CharSequence> header = newList();
        List<Iterable<CharSequence>> body = newList();
        for (Iterable<Cells> row: tableBody) {
            if (header.isEmpty()) {
                header = createHeader(tableHeader, row);
            }
            List<CharSequence> bodyRow = newList(flatMap(Cells_.<CharSequence>cells(), row));
            Assert.equal(header.size(), bodyRow.size());
            body.add(bodyRow);
        }
        
        return mkString("\r\n", map(Transformers.map(CsvConversionService_.escape).andThen(CsvConversionService_.joinCells), cons(header, body))).getBytes(Charset.forName("UTF-8"));
    }

    private List<CharSequence> createHeader(Iterable<String> tableHeader, Iterable<Cells> row) {
        List<CharSequence> header = newList();
        Iterator<String> fieldNames = tableHeader.iterator();
        for (Cells cells: row) {
            CharSequence currentFieldName = fieldNames.next();
            String unit = cells.unit.map(prepend(" (").andThen(append(")"))).getOrElse("");
            if (cells.headers.isEmpty()) {
                // skip
            } else if (cells.headers.equals(newList(""))) {
                header.add(currentFieldName + unit);
                for (@SuppressWarnings("unused") Object o: tail(cells.cells)) {
                    header.add("");
                }
            } else {
                Assert.equal(cells.cells.size(), cells.headers.size());
                header.addAll(newList(map(prepend(currentFieldName + " ").andThen(append(cells.headers.size() == 1 ? unit : "")), cells.headers)));
            }
        }
        return header;
    }
    
    /**
     * Remove newlines/cr:s and escape double quotes
     */
    static CharSequence escape(CharSequence str) {
        return str.toString().replace("\r", " ").replace("\n", " ").replace("\"", "\"\"");
    }
    
    static CharSequence joinCells(Iterable<CharSequence> str) {
        return '"' + mkString("\",\"", str) + '"';
    }
    
    private <K,V,O> Iterable<Iterable<Cells>> mapBody(final Map<K, ? extends Iterable<V>> obj, final Iterable<? extends MetaNamedMember<V, O>> members) {
        return map(CsvConversionService_.<K,V,O>mapBodyRow().ap(this, members), flatMap(CsvConversionService_.<K,V>flatKeyToValues(), obj.entrySet()));
    }
    
    <K,V,O> Iterable<Cells> mapBodyRow(Iterable<? extends MetaNamedMember<V, O>> members, K key, V value) {
        return cons(module.serialize(key), map(CsvModule_.serialize1().ap(module), map(CsvConversionService_.<V,O>foo().ap(value), members)));
    }
    
    @SuppressWarnings("unchecked")
    static <V,O> Pair<Object,Class<Object>> foo(V value, MetaNamedMember<V, O> member) {
        return (Pair<Object,Class<Object>>)(Object)Pair.of(member.apply(value), MemberUtil.actualTypeUnwrappingOptionAndEither(member));
    }
    
    static <K,V> Iterable<Map.Entry<K,V>> flatKeyToValues(K key, Iterable<V> values) {
        return map(CsvConversionService_.<K,V>makePair().ap(key), values);
    }
    
    static <K,V> Map.Entry<K,V> makePair(K key, V value) {
        return Pair.of(key, value);
    }
    
    private static <T> Iterable<String> header(final Iterable<? extends MetaNamedMember<T, ?>> members) {
        return map(MemberUtil_.memberName, members);
    }
    
    final <T> Iterable<Cells> regularBodyRow(final Iterable<? extends MetaNamedMember<T, ?>> members, final T obj) {
        return map(CsvConversionService_.<T>cell().ap(this, obj), members);
    }
    
    final <T> Cells cell(final T obj, final MetaNamedMember<T, ?> member) {
        if (member.getName().equals("")) {
            // oma dummy-otsikko
            return module.serialize(member.apply(obj));
        } else {
            return module.serialize(member.apply(obj), MemberUtil.actualTypeUnwrappingOptionAndEither(member));
        }
    }
}
