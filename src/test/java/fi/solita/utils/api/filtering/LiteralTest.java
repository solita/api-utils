package fi.solita.utils.api.filtering;

import static org.junit.Assert.*;

import org.junit.Test;

public class LiteralTest {

    @Test
    public void functionCall() {
        Literal.of("foo()");
    }
    
    @Test
    public void functionCallWithArgument() {
        Literal.of("foo(bar)");
    }
    
    @Test
    public void functionCallWithArithmetic() {
        Literal.of("foo()+2");
        Literal.of("foo()-2");
        Literal.of("foo()*2");
        Literal.of("foo()/2");
    }
    
    @Test
    public void functionCallWithArgumentAndArithmetic() {
        Literal.of("foo(bar)+2");
        Literal.of("foo(bar)-2");
        Literal.of("foo(bar)*2");
        Literal.of("foo(bar)/2");
    }
    
    @Test
    public void operatorAsLiteralString() {
        assertTrue(Literal.of("'+'").getValue().isLeft());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void regularAttributeFails() {
        Literal.of("foo");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void regularAttributeWithArithmeticFails() {
        Literal.of("foo*2");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void multipleArithmeticsFails() {
        Literal.of("foo*2*3");
    }
}
