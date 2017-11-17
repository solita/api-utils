package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.headOption;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.FunctionalC.mkString;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

public abstract class Assert {
    private Assert() {
        //
    }

    public static String notNullOrEmpty(String string) {
        notNull(string);
        notEmpty(string);
        return string;
    }

    public static String notEmpty(String string) {
        if (string.length() == 0) {
            throw new IllegalArgumentException("String is empty");
        }
        return string;
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

    public static <T> T Null(T o) {
        return Null(o, "Argument must be null");
    }

    public static <T> T Null(T o, CharSequence message) {
        if (o != null) {
            throw new IllegalArgumentException(message.toString());
        }
        return o;
    }

    public static int positive(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be > 0");
        }
        return number;
    }

    public static long positive(long number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be > 0");
        }
        return number;
    }

    public static BigDecimal positive(BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be > 0");
        }
        return number;
    }

    public static int nonnegative(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be >= 0");
        }
        return number;
    }

    public static long nonnegative(long number) {
        if (number < 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be >= 0");
        }
        return number;
    }

    public static BigDecimal nonnegative(BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be >= 0");
        }
        return number;
    }

    public static int negative(int number) {
        if (number >= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be < 0");
        }
        return number;
    }

    public static BigDecimal negative(BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException("Argument (" + number + ") must be < 0");
        }
        return number;
    }

    public static <T> T instance(T object, Class<?> expectedClass) {
        if (!expectedClass.isInstance(object)) {
            throw new IllegalArgumentException("Object '" + object + "' must be an instance of " + expectedClass);
        }
        return object;
    }

    public static <T> T notInstance(T object, Class<?> expectedClass) {
        if (expectedClass.isInstance(object)) {
            throw new IllegalArgumentException("Object '" + object + "' must NOT be an instance of " + expectedClass);
        }
        return object;
    }

    public static <T> List<T> noDuplicatesInList(List<T> list) {
        Set<T> set = new HashSet<T>();
        for (T t : list) {
            boolean notDuplicate = set.add(t);
            if (notDuplicate == false)
                throw new IllegalArgumentException("Duplicate element: " + t.toString());
        }
        return list;
    }

    public static void notEqual(Object o1, Object o2) {
        if (o1.equals(o2)) {
            throw new IllegalArgumentException(lazyToString("", "'", o1, "' must not equal '", o2, "'").toString());
        }
    }

    public static void notEqual(Object o1, Object o2, CharSequence message) {
        if (o1.equals(o2)) {
            throw new IllegalArgumentException(message.toString());
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

    public static <T extends Iterable<?>> T empty(T iterable) {
        return empty(iterable, "Given iterable was not empty");
    }

    public static <T extends Iterable<?>> T empty(T iterable, CharSequence message) {
        if (!isEmpty(iterable)) {
            throw new IllegalArgumentException(message.toString());
        }
        return iterable;
    }

    public static <T extends Iterable<?>> T notEmpty(T iterable) {
        return notEmpty(iterable, "Given iterable was empty");
    }

    public static <T extends Iterable<?>> T notEmpty(T iterable, CharSequence message) {
        if (isEmpty(iterable)) {
            throw new IllegalArgumentException(message.toString());
        }
        return iterable;
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

    public static boolean False(boolean condition, CharSequence message) {
        if (condition) {
            throw new IllegalArgumentException(message.toString());
        }
        return condition;
    }

    public static boolean False(boolean condition) {
        return False(condition, "Condition was true");
    }

    public static Object[] size(int expectedLength, Object[] array) {
        if (array.length != expectedLength) {
            throw new IllegalArgumentException("Array was not of expected length of " + expectedLength + ". Actual length was " + array.length + ": " + Arrays.toString(array));
        }
        return array;
    }

    public static <T extends Iterable<?>> T size(int expectedLength, T iterable) {
        int size = (int)Functional.size(iterable);
        if (size != expectedLength) {
            throw new IllegalArgumentException("Iterable was not of expected size of " + expectedLength + ". Actual size was " + size + ": " + newList(iterable));
        }
        return iterable;
    }

    public static <T extends Map<?,?>> T size(int expectedLength, T map) {
        size(expectedLength, map.entrySet());
        return map;
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

    public static <T> Option<T> singletonOrEmpty(Iterable<T> iterable) {
        if (iterable == null) {
            return null;
        }
        int size = (int)Functional.size(iterable);
        if (size > 1) {
            throw new IllegalArgumentException("Iterable was larger than size 1: " + newList(iterable));
        }
        return headOption(iterable);
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

    public static <T> Option<T> undefined(Option<T> option) {
        if (option.isDefined()) {
            throw new IllegalArgumentException(lazyToString("Value should not have been defined, but was: ", option).toString());
        }
        return option;
    }

    public static <T> Option<T> undefined(Option<T> option, CharSequence message) {
        if (option.isDefined()) {
            throw new IllegalArgumentException(message.toString());
        }
        return option;
    }

    @SuppressWarnings("unchecked")
    public static Option<?>[] exactlyOneDefined(Option<?>... options) {
        if (Functional.size(flatten(Arrays.asList((Option<Object>[])options))) != 1) {
            throw new IllegalArgumentException("Exactly one of the arguments should have been defined: " + options);
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    public static Option<?>[] atMostOneDefined(Option<?>... options) {
        if (Functional.size(flatten(Arrays.asList((Option<Object>[])options))) > 1) {
            throw new IllegalArgumentException("Not more than one should be defined " + Arrays.toString(options));
        }
        return options;
    }

    public static <K,V,M extends Map<K,V>> M mapContainsKeys(Set<K> keys, M map) {
        for (K key : keys) {
            if (!map.containsKey(key)) {
                throw new IllegalArgumentException("Missing key: " + key);
            }
        }
        return map;
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

    public static <T, C extends Set<T>> C contains(C c, T t) {
        if (!c.contains(t)) {
            throw new IllegalArgumentException(c + " must contain " + t);
        }
        return c;
    }

    public static <T extends Comparable<T>> void betweenInclusive(T value, T startInclusive, T endInclusive) {
        greaterThanOrEqual(value, startInclusive);
        lessThanOrEqual(value, endInclusive);
    }

    public static void shouldNotBeHere() {
        throw new RuntimeException("Should not be here");
    }

    private static final Pattern number = Pattern.compile("[0-9]+");

    public static String isNumber(String str) {
        True(number.matcher(str).matches());
        return str;
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
