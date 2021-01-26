package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.FunctionalC.init;
import static fi.solita.utils.functional.FunctionalC.tail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.Tuple3;

public abstract class Literal {
    public static final String OPERATORS = Pattern.quote("+") + Pattern.quote("-") + Pattern.quote("*") + Pattern.quote("/");
    
    private static final Pattern PLAIN_LITERAL = Pattern.compile(FilterParser.plainLiteral);
    private static final Pattern WKT_LITERAL = Pattern.compile(FilterParser.polygon);
    public static final Pattern ARITHMETIC = Pattern.compile("(" + FilterParser.plainLiteral + ")([" + OPERATORS + "])(" + FilterParser.plainLiteral + ")");
    
    public abstract Either<String,Tuple3<String,Character,String>> getValue();
    
    private static final String stripLiteral(String expr) {
        return (expr.startsWith("'") && expr.endsWith("'") ? init(tail(expr)) : expr).replace("''", "'");
    }
    
    public static final Literal of(String literal) {
        if (literal == null) {
            return null;
        }
        Matcher m = ARITHMETIC.matcher(literal);
        if (m.matches()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String g3 = m.group(3);
            return new ArithmeticLiteral(stripLiteral(g1), g2.charAt(0), stripLiteral(g3));
        } else if (PLAIN_LITERAL.matcher(literal).matches()) {
            return new RegularLiteral(stripLiteral(literal));
        } else if (WKT_LITERAL.matcher(literal).matches()) {
            return new RegularLiteral(literal);
        } else {
            throw new IllegalArgumentException(literal);
        }
    }
    
    private static final class RegularLiteral extends Literal {
        public final String value;

        public RegularLiteral(String value) {
            this.value = value;
        }
        
        @Override
        public Either<String, Tuple3<String, Character, String>> getValue() {
            return Either.left(value);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Literal.RegularLiteral other = (Literal.RegularLiteral) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "RegularLiteral [value=" + value + "]";
        }
    }
    
    private static final class ArithmeticLiteral extends Literal {
        public final String value1;
        public final char op;
        public final String value2;
        
        public ArithmeticLiteral(String value1, char op, String value2) {
            this.value1 = value1;
            this.op = op;
            this.value2 = value2;
        }
        
        @Override
        public Either<String, Tuple3<String, Character, String>> getValue() {
            return Either.right(Tuple.of(value1, op, value2));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + op;
            result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
            result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ArithmeticLiteral other = (ArithmeticLiteral) obj;
            if (op != other.op)
                return false;
            if (value1 == null) {
                if (other.value1 != null)
                    return false;
            } else if (!value1.equals(other.value1))
                return false;
            if (value2 == null) {
                if (other.value2 != null)
                    return false;
            } else if (!value2.equals(other.value2))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ArithmeticLiteral [value1=" + value1 + ", op=" + op + ", value2=" + value2 + "]";
        }
    }
}