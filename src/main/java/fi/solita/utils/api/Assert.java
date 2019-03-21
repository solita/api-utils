package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.FunctionalC.mkString;

import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

public abstract class Assert {
    private Assert() {
        //
    }
    
    public static int positive(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be > 0");
        }
        return number;
    }
    
    public static <T> T Null(T o) {
        return Null(o, "Argument must be null");
    }
    
    public static <T> T Null(T o, CharSequence message) {
        if (o != null) {
            throw new IllegalArgumentException(message.toString());
        }
        return o;
    }

    public static <T> T notNull(T o) {
        return notNull(o, "Argument cannot be null");
    }

    public static <T> T notNull(T o, CharSequence message) {
        if (o == null) {
            throw new IllegalArgumentException(message.toString());
        }
        return o;
    }

    public static void notEqual(Object o1, Object o2) {
        if (o1.equals(o2)) {
            throw new IllegalArgumentException(lazyToString("'", o1, "' must not equal '", o2, "'").toString());
        }
    }
    
    public static void equal(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new IllegalArgumentException(lazyToString("'", o1, "' must equal '", o2, "'").toString());
        }
    }

    public static void equal(Object o1, Object o2, CharSequence message) {
        if (!o1.equals(o2)) {
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static boolean True(boolean condition, CharSequence message) {
        if (!condition) {
            throw new IllegalArgumentException(message.toString());
        }
        return condition;
    }

    public static boolean True(boolean condition) {
        return True(condition, "Condition was false");
    }
    
    public static <T extends Comparable<T>> void greaterThan(T c1, T c2) {
        if (c1.compareTo(c2) <= 0) {
            throw new IllegalArgumentException(c1 + " must be greater than " + c2);
        }
    }


    public static <T extends Comparable<T>> void greaterThanOrEqual(T c1, T c2) {
        if (c1.compareTo(c2) < 0) {
            throw new IllegalArgumentException(c1 + " must be greater than or equal to " + c2);
        }
    }

    public static <T extends Comparable<T>> void lessThan(T c1, T c2) {
        if (c1.compareTo(c2) >= 0) {
            throw new IllegalArgumentException(c1 + " must be less than " + c2);
        }
    }

    public static <T extends Comparable<T>> void lessThanOrEqual(T c1, T c2) {
        if (c1.compareTo(c2) > 0) {
            throw new IllegalArgumentException(c1 + " must be less than or equal to " + c2);
        }
    }
    
    public static <T> T singleton(Iterable<T> iterable) {
        if (iterable == null) {
            return null;
        }
        int size = (int)Functional.size(iterable);
        if (size != 1) {
            throw new IllegalArgumentException("Iterable was not of size 1: " + newList(iterable));
        }
        return head(iterable);
    }

    public static <T> Option<T> defined(Option<T> option) {
        return defined(option, "Value should have been defined");
    }

    public static <T> Option<T> defined(Option<T> option, CharSequence message) {
        if ( !option.isDefined() ) {
            throw new IllegalArgumentException(message.toString());
        }
        return option;
    }

    public static CharSequence lazyToString(final Object... charSequences) {
        return new CharSequence() {
            private StringBuilder res = null;

            @Override
            public CharSequence subSequence(int paramInt1, int paramInt2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int length() {
                force();
                return res.length();
            }

            private void force() {
                if (res == null) {
                    res = new StringBuilder();
                    for (Object o: charSequences) {
                        res.append(o.toString());
                    }
                }
            }

            @Override
            public char charAt(int paramInt) {
                force();
                return res.charAt(paramInt);
            }

            @Override
            public String toString() {
                return mkString("", this);
            }
        };
    }
}
