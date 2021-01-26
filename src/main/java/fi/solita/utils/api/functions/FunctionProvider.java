package fi.solita.utils.api.functions;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.base.AbstractInterval;

import fi.solita.utils.api.filtering.FilterParser;
import fi.solita.utils.api.filtering.Literal;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class FunctionProvider {
    private static final Pattern PATTERN = Pattern.compile("([^(]+)[(]([^)]*)[)]|(" + FilterParser.plainLiteral + "){1}+([" + Literal.OPERATORS + "])(" + FilterParser.plainLiteral + ")");
    
    public static class UnknownFunctionException extends RuntimeException {
        public final String functionName;
        public UnknownFunctionException(String name) {
            super("Unknown function: " + name);
            this.functionName = name;
        }
    }
    
    public static class UnsupportedFunctionForPropertyException extends RuntimeException {
        public final String functionName;
        public final Option<String> propertyName;
        
        public UnsupportedFunctionForPropertyException(String functionName, Option<String> propertyName) {
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
    
    private static String getFunctionName(Matcher m) {
        return Option.of(m.group(1)).getOrElse(m.group(4));
    }
    
    public String mapArgument(String str, Apply<? super String,String> f) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(getFunctionName(m));
            if (m.group(4) != null) {
                return f.apply(m.group(3)) + getFunctionName(m) + f.apply(m.group(5));
            } else {
                return getFunctionName(m) + "(" + f.apply(m.group(2)) + ")";
            }
        } else {
            return str;
        }
    }

    public String toArgument(String str) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(getFunctionName(m));
            Assert.Null(m.group(4));
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
        } else if ("duration".equals(functionName)) {
            // ok
        } else if ("+".equals(functionName)) {
            // ok
        } else if ("-".equals(functionName)) {
            // ok
        } else if ("*".equals(functionName)) {
            // ok
        } else if ("/".equals(functionName)) {
            // ok
        } else {
            throw new UnknownFunctionException(functionName);
        }
    }
    
    public boolean argumentMatches(String str, Apply<String,Boolean> f) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            assertKnownFunction(getFunctionName(m));
            if (m.group(4) != null) {
                return f.apply(m.group(3)) || f.apply(m.group(5));
            } else {
                return f.apply(m.group(2));
            }
        } else {
            return f.apply(str);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Option<Class<?>> changesResultType(String str) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            String functionName = getFunctionName(m);
            if ("start".equals(functionName)) {
                return (Option<Class<?>>)(Object)Some(DateTime.class);
            } else if ("end".equals(functionName)) {
                return (Option<Class<?>>)(Object)Some(DateTime.class);
            } else if ("duration".equals(functionName)) {
                return (Option<Class<?>>)(Object)Some(Duration.class);
            }
        }
        return None(); 
    }
    
    public Object apply(String str, Object value) {
        Matcher m = FunctionProvider.PATTERN.matcher(str);
        if (m.matches()) {
            return applyFunction(getFunctionName(m), Option.of(m.group(2)), value);
        } else {
            return value;
        }
    }
    
    public Object applyFunction(String functionName, Option<String> propertyName, Object value) {
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
        } else if ("duration".equals(functionName)) {
            if (value instanceof Interval) {
                return ((AbstractInterval) value).toDuration();
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("+".equals(functionName) && value instanceof Pair) {
            Object left  = ((Pair<?,?>)value).left();
            Object right = ((Pair<?,?>)value).right();
            if (isInteger(left) && isInteger(right)) {
                BigInteger result = new BigInteger(left.toString()).add(new BigInteger(right.toString()));
                return left instanceof BigInteger ? result : left.getClass().cast(result.longValue());
            } else if (isFloat(left) && isFloat(right)) {
                return left instanceof Float ? (float)left + (float)right : (double)left + (double)right;
            } else if (left instanceof BigDecimal && right instanceof BigDecimal) {
                return ((BigDecimal)left).add((BigDecimal) right);
            } else if (left instanceof DateTime && right instanceof Duration) {
                return ((DateTime)left).plus((Duration)right);
            } else if (left instanceof DateTime && right instanceof Period) {
                return ((DateTime)left).plus((Period)right);
            } else if (left instanceof Interval && right instanceof Duration) {
                Interval i = (Interval)left;
                Duration d = (Duration)right;
                return i.withEnd(i.getEnd().plus(d)).withStart(i.getStart().plus(d));
            } else if (left instanceof Interval && right instanceof Period) {
                Interval i = (Interval)left;
                Period d = (Period)right;
                return i.withEnd(i.getEnd().plus(d)).withStart(i.getStart().plus(d));
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("-".equals(functionName) && value instanceof Pair) {
            Object left  = ((Pair<?,?>)value).left();
            Object right = ((Pair<?,?>)value).right();
            if (isInteger(left) && isInteger(right)) {
                BigInteger result = new BigInteger(left.toString()).subtract(new BigInteger(right.toString()));
                return left instanceof BigInteger ? result : left.getClass().cast(result.longValue());
            } else if (isFloat(left) && isFloat(right)) {
                return left instanceof Float ? (float)left - (float)right : (double)left - (double)right;
            } else if (left instanceof BigDecimal && right instanceof BigDecimal) {
                return ((BigDecimal)left).subtract((BigDecimal) right);
            } else if (left instanceof DateTime && right instanceof Duration) {
                return ((DateTime)left).minus((Duration)right);
            } else if (left instanceof DateTime && right instanceof Period) {
                return ((DateTime)left).minus((Period)right);
            } else if (left instanceof Interval && right instanceof Duration) {
                Interval i = (Interval)left;
                Duration d = (Duration)right;
                return i.withStart(i.getStart().minus(d)).withEnd(i.getEnd().minus(d));
            } else if (left instanceof Interval && right instanceof Period) {
                Interval i = (Interval)left;
                Period d = (Period)right;
                return i.withStart(i.getStart().minus(d)).withEnd(i.getEnd().minus(d));
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("*".equals(functionName) && value instanceof Pair) {
            Object left  = ((Pair<?,?>)value).left();
            Object right = ((Pair<?,?>)value).right();
            if (isInteger(left) && isInteger(right)) {
                BigInteger result = new BigInteger(left.toString()).multiply(new BigInteger(right.toString()));
                return left instanceof BigInteger ? result : left.getClass().cast(result.longValue());
            } else if (isFloat(left) && isFloat(right)) {
                return left instanceof Float ? (float)left * (float)right : (double)left * (double)right;
            } else if (left instanceof BigDecimal && right instanceof BigDecimal) {
                return ((BigDecimal)left).multiply((BigDecimal) right);
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else if ("/".equals(functionName) && value instanceof Pair) {
            Object left  = ((Pair<?,?>)value).left();
            Object right = ((Pair<?,?>)value).right();
            if (isInteger(left) && isInteger(right)) {
                BigInteger result = new BigInteger(left.toString()).divide(new BigInteger(right.toString()));
                return left instanceof BigInteger ? result : left.getClass().cast(result.longValue());
            } else if (isFloat(left) && isFloat(right)) {
                return left instanceof Float ? (float)left / (float)right : (double)left / (double)right;
            } else if (left instanceof BigDecimal && right instanceof BigDecimal) {
                return ((BigDecimal)left).divide((BigDecimal) right, RoundingMode.HALF_UP);
            }
            throw new UnsupportedFunctionForPropertyException(functionName, propertyName);
        } else {
            throw new UnknownFunctionException(functionName);
        }
    }
    
    private static boolean isInteger(Object n) {
        return n instanceof Short || n instanceof Integer || n instanceof Long || n instanceof BigInteger;
    }
    
    private static boolean isFloat(Object n) {
        return n instanceof Float || n instanceof Double;
    }
}