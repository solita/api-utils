package fi.solita.utils.api.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.functional.Pair;

public class PropertyNameTest {

    public static final PropertyName empty = PropertyName.of("");
    
    public static final PropertyName regular = PropertyName.of("foo");
    public static final PropertyName dateLiteral = PropertyName.of("2014-02-14");
    public static final PropertyName intervalLiteral = PropertyName.of("2014-02-14T00:00:00Z/2014-02-14T00:00:00Z");
    public static final PropertyName regularDeep = PropertyName.of("foo.bar");
    public static final PropertyName regularExclusion = PropertyName.of("-foo");
    public static final PropertyName regularDeepExclusion = PropertyName.of("foo.-bar");
    
    public static final PropertyName function = PropertyName.of("round(foo)");
    public static final PropertyName functionDeep = PropertyName.of("round(foo.bar)");
    public static final PropertyName arithmetic = PropertyName.of("2+1");
    
    public static final FunctionProvider fp = new FunctionProvider();
    
    @Test
    public void applyFunction_regularApplicationIsIdentity() {
        Object obj = new Object();
        assertEquals(obj, regular.applyFunction(fp, obj));
        assertEquals(obj, dateLiteral.applyFunction(fp, obj));
        assertEquals(obj, intervalLiteral.applyFunction(fp, obj));
        assertEquals(obj, regularDeep.applyFunction(fp, obj));
        assertEquals(obj, regularExclusion.applyFunction(fp, obj));
        assertEquals(obj, regularDeepExclusion.applyFunction(fp, obj));
        assertEquals(obj, empty.applyFunction(fp, obj));
    }
    
    @Test
    public void applyFunction() {
        assertEquals(42.0, function.applyFunction(fp, 42.2));
        assertEquals(42.0, functionDeep.applyFunction(fp, 42.2));
        assertEquals(43.0, arithmetic.applyFunction(fp, Pair.of(42.0, 1.0)));
    }
    
    @Test
    public void isEmpty() {
        assertFalse(regular.isEmpty(fp));
        assertFalse(dateLiteral.isEmpty(fp));
        assertFalse(intervalLiteral.isEmpty(fp));
        assertFalse(regularDeep.isEmpty(fp));
        assertFalse(regularExclusion.isEmpty(fp));
        assertFalse(regularDeepExclusion.isEmpty(fp));
        assertFalse(function.isEmpty(fp));
        assertFalse(functionDeep.isEmpty(fp));
        assertFalse(arithmetic.isEmpty(fp));
        
        assertTrue(empty.isEmpty(fp));
    }
    
    @Test
    public void isExclusion() {
        assertTrue(regularExclusion.isExclusion());
        
        // not an exclusion on this level
        assertFalse(regularDeepExclusion.isExclusion());
        
        assertFalse(regular.isExclusion());
        assertFalse(dateLiteral.isExclusion());
        assertFalse(intervalLiteral.isExclusion());
        assertFalse(regularDeep.isExclusion());
        assertFalse(function.isExclusion());
        assertFalse(functionDeep.isExclusion());
        assertFalse(arithmetic.isExclusion());
        assertFalse(empty.isExclusion());
    }
    
    @Test
    public void isFunctionCall() {
        assertFalse(regular.isFunctionCall());
        assertFalse(dateLiteral.isFunctionCall());
        assertFalse(intervalLiteral.isFunctionCall());
        assertFalse(regularDeep.isFunctionCall());
        assertFalse(regularExclusion.isFunctionCall());
        assertFalse(regularDeepExclusion.isFunctionCall());
        
        assertTrue(function.isFunctionCall());
        assertTrue(arithmetic.isFunctionCall());
        assertTrue(functionDeep.isFunctionCall());
        
        assertFalse(empty.isFunctionCall());
    }
    
    @Test
    public void isPrefixOf() {
        assertTrue(regular.isPrefixOf(fp, regularDeep.value));
        assertTrue(regular.isPrefixOf(fp, regularDeepExclusion.value));
        
        assertTrue(function.isPrefixOf(fp, regularDeep.value));
        
        assertTrue(empty.isPrefixOf(fp, empty.value));
        
        assertFalse(regularDeep.isPrefixOf(fp, regular.value));
        assertFalse(regularDeepExclusion.isPrefixOf(fp, regular.value));
        
        assertFalse(regularDeep.isPrefixOf(fp, function.value));
        
        assertFalse(empty.isPrefixOf(fp, regular.value));
        assertFalse(empty.isPrefixOf(fp, regularDeep.value));
        assertFalse(empty.isPrefixOf(fp, regularExclusion.value));
        assertFalse(empty.isPrefixOf(fp, function.value));
        assertFalse(empty.isPrefixOf(fp, arithmetic.value));
        assertFalse(empty.isPrefixOf(fp, functionDeep.value));
    }
    
    @Test
    public void omitExclusion() {
        assertEquals(regular, regularExclusion.omitExclusion());
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_regular() {
        regular.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_dateLiteral() {
        dateLiteral.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_intervalLiteral() {
        intervalLiteral.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_regularDeep() {
        regularDeep.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_regularDeepExclusion() {
        regularDeepExclusion.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_function() {
        function.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_arithmetic() {
        arithmetic.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_functionDeep() {
        functionDeep.omitExclusion();
    }
    
    @Test(expected = IllegalStateException.class)
    public void omitExclusion_empty() {
        empty.omitExclusion();
    }
    
    @Test
    public void stripPrefix() {
        assertEquals(empty, regular         .stripPrefix(fp, regular.value));
        assertEquals(empty, dateLiteral     .stripPrefix(fp, dateLiteral.value));
        assertEquals(empty, intervalLiteral     .stripPrefix(fp, intervalLiteral.value));
        assertEquals(empty, empty           .stripPrefix(fp, empty.value));

        assertEquals(regularExclusion, regularExclusion.stripPrefix(fp, regular.value));
        
        assertEquals(PropertyName.of("bar") , regularDeep.stripPrefix(fp, "foo"));
        assertEquals(PropertyName.of("-bar"), regularDeepExclusion.stripPrefix(fp, "foo"));
        
        assertEquals(PropertyName.of("round()")   , function    .stripPrefix(fp, "foo"));
        assertEquals(PropertyName.of("round(bar)"), functionDeep.stripPrefix(fp, "foo"));
    }
    
    @Test
    public void toProperty() {
        assertEquals(regular             , regular.toProperty(fp));
        assertEquals(dateLiteral         , dateLiteral.toProperty(fp));
        assertEquals(intervalLiteral     , intervalLiteral.toProperty(fp));
        assertEquals(regularDeep         , regularDeep.toProperty(fp));
        assertEquals(regularExclusion    , regularExclusion.toProperty(fp));
        assertEquals(regularDeepExclusion, regularDeepExclusion.toProperty(fp));
        assertEquals(empty               , empty.toProperty(fp));
        
        assertEquals(regular             , function.toProperty(fp));
        assertEquals(regularDeep         , functionDeep.toProperty(fp));
    }
}
