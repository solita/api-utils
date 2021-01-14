package fi.solita.utils.api.functions;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.base.AbstractInterval;

import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;

public class FunctionProvider {
    private static final Pattern PATTERN = Pattern.compile("([^(]+)[(]([^)]*)[)]");
    
    public static class UnknownFunctionException extends RuntimeException {
        public final String functionName;
        public UnknownFunctionException(String name) {
            super("Unknown function: " + name);
            this.functionName = name;
        }
    }
    
    public static class UnsupportedFunctionForPropertyException extends RuntimeException {
        public final String functionName;
        public final String propertyName;
        
        public UnsupportedFunctionForPropertyException(String functionName, String propertyName) {
            super("Funktiota " + functionName + " ei voi käyttää parametrille " + propertyName);
            this.functionName = functionName;
            this.propertyName = propertyName;
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
        public Object apply(String str, Object value) {
            return value;
        }
    };
    
    public static boolean isFunctionCall(String str) {
        return FunctionProvider.PATTERN.matcher(str).matches();
    }
    
    public String mapArgument(String str, Apply<? super String,String> f) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(m.group(1));
            return m.group(1) + "(" + f.apply(m.group(2)) + ")";
        } else {
            return str;
        }
    }
    
    public String toArgument(String str) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(m.group(1));
            return m.group(2);
        } else {
            return str;
        }
    }
    
    protected void assertKnownFunction(String functionName) {
        if ("round".equals(functionName)) {
            // ok
        } else if ("start".equals(functionName)) {
            // ok
        } else if ("end".equals(functionName)) {
            // ok
        } else {
            throw new UnknownFunctionException(functionName);
        }
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
    
    @SuppressWarnings("unchecked")
    public Option<Class<?>> changesResultType(String str) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            String functionName = m.group(1);
            if ("start".equals(functionName)) {
                return (Option<Class<?>>)(Object)Some(DateTime.class);
            } else if ("end".equals(functionName)) {
                return (Option<Class<?>>)(Object)Some(DateTime.class);
            }
        }
        return None(); 
    }
    
    public Object apply(String str, Object value) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            return applyFunction(m.group(1), m.group(2), value);
        } else {
            return value;
        }
    }
    
    protected Object applyFunction(String functionName, String propertyName, Object value) {
        if ("round".equals(functionName)) {
            if (value instanceof Float) {
                return Float.valueOf(((Float) value).longValue());
            } else if (value instanceof Double) {
                return Double.valueOf(((Double) value).longValue());
            } else if (value instanceof BigDecimal) {
                return BigDecimal.valueOf(((BigDecimal) value).longValue());
            } else if (value instanceof Integer) {
                return value;
            } else if (value instanceof Long) {
                return value;
            } else if (value instanceof Short) {
                return value;
            } else if (value instanceof BigInteger) {
                return value;
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("start".equals(functionName)) {
            if (value instanceof Interval) {
                return ((AbstractInterval) value).getStart();
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("end".equals(functionName)) {
            if (value instanceof Interval) {
                return ((AbstractInterval) value).getEnd();
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else {
            throw new UnknownFunctionException(functionName);
        }
    }
}