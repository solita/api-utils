package fi.solita.utils.api;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.solita.utils.functional.Apply;

public class FunctionProvider {
    private static final Pattern PATTERN = Pattern.compile("([^(]+)[(]([^)]*)[)]");
    
    public static class UnknownFunctionException extends RuntimeException {
        public UnknownFunctionException(String name) {
            super("Unknown function: " + name);
        }
    }
    
    public static final FunctionProvider NONE = new FunctionProvider() {
        @Override
        public String mapArgument(String str, Apply<? super String, String> f) {
            return f.apply(str);
        }
        
        @Override
        public boolean argumentMatches(String str, Apply<String, Boolean> f) {
            return f.apply(str);
        }
        
        @Override
        public Object apply(PropertyName str, Object value) {
            return value;
        }
    };
    
    public String mapArgument(String str, Apply<? super String,String> f) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(m.group(1));
            return m.group(1) + "(" + f.apply(m.group(2)) + ")";
        } else {
            return str;
        }
    }
    
    protected void assertKnownFunction(String functionName) {
        if ("round".equals(functionName)) {
            // ok
        } else {
            throw new UnknownFunctionException(functionName);
        }
    }
    
    public boolean isFunctionCall(PropertyName propertyName) {
        Matcher m = FunctionProvider.PATTERN.matcher(propertyName.value);
        return m.matches();
    }
    
    public boolean argumentMatches(String str, Apply<String,Boolean> f) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(m.group(1));
            return f.apply(m.group(2));
        } else {
            return f.apply(str);
        }
    }
    
    public Object apply(PropertyName str, Object value) {
        Matcher m = FunctionProvider.PATTERN.matcher(str.value);
        if (m.matches()) {
            String function = m.group(1);
            if ("round".equals(function)) {
                if (value instanceof Float) {
                    return Float.valueOf(((Number) value).longValue());
                } else if (value instanceof Double) {
                    return Double.valueOf(((Number) value).longValue());
                } else if (value instanceof BigDecimal) {
                    return BigDecimal.valueOf(((BigDecimal) value).longValue());
                }
            } else {
                throw new UnknownFunctionException(m.group(1));
            }
            
            throw new UnsupportedOperationException("Don't know how to apply " + m.group(1) + " to a " + value.getClass().getName());
        } else {
            return str;
        }
    }
}