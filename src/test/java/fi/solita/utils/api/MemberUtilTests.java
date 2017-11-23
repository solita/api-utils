package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;

import fi.solita.utils.functional.Transformer;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtilTests {

    @SuppressWarnings("unused")
    private static class FooDto {
        public String bar;
        public String getBar() {
            return bar;
        }
        public final String someOtherMethod() {
            return null;
        }
    }
    
    @SuppressWarnings("unused")
    private static class BarDto {
        public boolean foo;
        public boolean isFoo() {
            return foo;
        }
    }
    
    @Test
    public void allMethods_returnsAllMethods() throws Exception {
        Collection<? extends MetaNamedMember<? super FooDto,?>> methods = MemberUtil.allMethods(FooDto.class);
        
        assertEquals(2, methods.size());
        assertEquals(newSet("getBar", "someOtherMethod"), newSet(map(new Transformer<MetaNamedMember<?,?>, String>() {
            @Override
            public String transform(MetaNamedMember<?, ?> source) {
                return source.getName();
            }
        }, methods)));
    }
    
    @Test
    public void beanGetters_doesNotReturnNonBeanNamed() throws Exception {
        Collection<? extends MetaNamedMember<? super FooDto,?>> methods = MemberUtil.beanGetters(FooDto.class);
        
        assertEquals(1, methods.size());
        assertEquals("bar", head(methods).getName());
    }
    
    @Test
    public void beanGetters_returnsIs() throws Exception {
        Collection<? extends MetaNamedMember<? super BarDto,?>> methods = MemberUtil.beanGetters(BarDto.class);
        
        assertEquals(1, methods.size());
        assertEquals("foo", head(methods).getName());
    }
}
