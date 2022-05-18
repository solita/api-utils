package fi.solita.utils.api.base.http;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.find;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.convert.converter.Converter;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.filtering.Filter;
import fi.solita.utils.api.filtering.FilterParser;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.types.SRSName_;
import fi.solita.utils.api.types.StartIndex;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class HttpSerializers {
    
    @SuppressWarnings("unused")
    private final Serializers s;

    public HttpSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    
    
    /**
     * Some primitive serializers, to be used as helper functions for actual serialization
     */
    
    public static final <T> Converter<String,T> converter(final Apply<String,T> f) {
        return new Converter<String,T>() {
            @Override
            public T convert(String source) {
                return f.apply(source);
            }
        };
    }
    
    public static final <E extends Enum<E>> Converter<String, E> enumConverter(final Class<E> enumClass, final Apply<E,String> serialization) {
        return new Converter<String, E>() {
            @Override
            public E convert(String source) {
                for (E v: enumClass.getEnumConstants()) {
                    if (serialization.apply(v).equals(source)) {
                        return v;
                    }
                }
                throw new InvalidValueException("value", source, newList(map(serialization, enumClass.getEnumConstants())));
            }
        };
    }
    
    
    
    
    
    /**
     * Some concrete serializers for common types
     */
    
    /**
     * Generic invalid-value error for ordinary cases
     */
    public static class InvalidValueException extends RuntimeException {
        public final String type;
        public final String value;
        public final Collection<String> validValues;
        public InvalidValueException(String type, String value, Collection<String> validValues) {
            super(value);
            this.type = type;
            this.value = value;
            this.validValues = validValues;
        }
        public InvalidValueException(String type, String value) {
            this.type = type;
            this.value = value;
            this.validValues = emptyList();
        }
    }
    
    public static final class InvalidFilterException extends RuntimeException {
        public final List<String> validValues;
        public InvalidFilterException(List<String> validValues) {
            this.validValues = validValues;
        }
    }
    
    public static final class InvalidStartIndexException extends RuntimeException {
    }
    
    public static final class BeginAndEndMustMatchException extends RuntimeException {}
    public static final class IntervalNotWithinLimitsException extends RuntimeException {
        public final String validStart;
        public final String validEnd;
        public IntervalNotWithinLimitsException(String validStart, String validEnd) {
            this.validStart = validStart;
            this.validEnd = validEnd;
        }
    }
    
    public static final class LocalDateNotWithinLimitsException extends RuntimeException {
        public final String validStart;
        public final String validEnd;
        public LocalDateNotWithinLimitsException(String validStart, String validEnd) {
            this.validStart = validStart;
            this.validEnd = validEnd;
        }
    }
    
    public static class InvalidTimeZoneException extends RuntimeException {
        public final String timeZone;
        public InvalidTimeZoneException(String timeZone) {
            super(timeZone);
            this.timeZone = timeZone;
        }
    }
    
    public static final class DateTimeNotWithinLimitsException extends RuntimeException {
        public final String validStart;
        public final String validEnd;
        public DateTimeNotWithinLimitsException(String validStart, String validEnd) {
            this.validStart = validStart;
            this.validEnd = validEnd;
        }
    }
    
    private final Converter<String,Revision> revision = new Converter<String, Revision>() {
        @Override
        public Revision convert(String source) throws InvalidValueException {
            try {
                long val = Long.parseLong(source);
                return new Revision(val);
            } catch (RuntimeException e) {
                throw new InvalidValueException("revision", source);
            }
        }
    };
    
    private final Converter<String,PropertyName> propertyName = converter(PropertyName_.of);
    
    private final Converter<String,Filters> filter = new Converter<String, Filters>() {
        @Override
        public Filters convert(String source) {
            List<Filter> filters = FilterParser.parse(source);
            if (!filters.isEmpty()) {
                return new Filters(filters);
            }
            throw new InvalidFilterException(Filters.SUPPORTED_OPERATIONS);
        }
    };
    
    private final Converter<String,Boolean> bool = new Converter<String, Boolean>() {
        @Override
        public Boolean convert(String source) {
            return Boolean.parseBoolean(source);
        }
    };
    
    private final Converter<String,Character> character = new Converter<String, Character>() {
        @Override
        public Character convert(String source) {
            Assert.True(source.length() == 1);
            return source.charAt(0);
        }
    };
    
    private final Converter<String,Short> _short = new Converter<String, Short>() {
        @Override
        public Short convert(String source) {
            return Short.parseShort(source);
        }
    };
    
    private final Converter<String,Integer> _int = new Converter<String, Integer>() {
        @Override
        public Integer convert(String source) {
            return Integer.parseInt(source);
        }
    };
    
    private final Converter<String,Long> _long = new Converter<String, Long>() {
        @Override
        public Long convert(String source) {
            return Long.parseLong(source);
        }
    };
    
    private final Converter<String,BigInteger> biginteger = new Converter<String, BigInteger>() {
        @Override
        public BigInteger convert(String source) {
            return new BigInteger(source);
        }
    };
    
    private final Converter<String,BigDecimal> bigdecimal = new Converter<String, BigDecimal>() {
        @Override
        public BigDecimal convert(String source) {
            return new BigDecimal(source);
        }
    };
    
    private final Converter<String,Count> count = new Converter<String, Count>() {
        @Override
        public Count convert(String source) throws InvalidValueException {
            try {
                int val = Integer.parseInt(source);
                Assert.True(Count.validValues.contains(val));
                return new Count(val);
            } catch (RuntimeException e) {
                throw new InvalidValueException("count", source, newList(map(HttpSerializers_.int2string, Count.validValues)));
            }
        }
    };
    
    static String int2string(Integer i) {
        return Integer.toString(i);
    }
    
    private final Converter<String,StartIndex> startIndex = new Converter<String, StartIndex>() {
        @Override
        public StartIndex convert(String source) throws InvalidStartIndexException {
            try {
                int val = Integer.parseInt(source);
                return new StartIndex(val);
            } catch (RuntimeException e) {
                throw new InvalidStartIndexException();
            }
        }
    };
    
    private final Converter<String,SRSName> srsName = new Converter<String, SRSName>() {
        @Override
        public SRSName convert(String source) throws InvalidValueException {
            try {
                Option<SRSName> found = find(SRSName_.value.andThen(equalTo(source)), SRSName.validValues);
                Assert.defined(found);
                return found.get();
            } catch (RuntimeException e) {
                throw new InvalidValueException("srs", source, newList(map(SRSName_.value, SRSName.validValues)));
            }
        }
    };
    
    private final Converter<String,DateTime> ajanhetki = new Converter<String, DateTime>() {
        private final DateTime VALID_BEGIN = VALID.getStart();
        private final DateTime VALID_END = VALID.getEnd();
        
        @Override
        public DateTime convert(String source) throws InvalidValueException, DateTimeNotWithinLimitsException {
            DateTime ret;
            try {
                ret = dateTimeParser.parseDateTime(source);
            } catch (Exception e) {
                throw new InvalidValueException("datetime", source);
            }
            
            if (ret.isBefore(VALID_BEGIN) || ret.isAfter(VALID_END)) {
                throw new IntervalNotWithinLimitsException(dateTimeParser.print(VALID_BEGIN), dateTimeParser.print(VALID_END));
            }
            
            return ret;
        }
    };
    
    private final Converter<String,Interval> interval = new Converter<String, Interval>() {
        @Override
        public Interval convert(String source) throws InvalidValueException, IntervalNotWithinLimitsException {
            String[] parts = source.split("/");
            
            Interval ret;
            try {
                Assert.True(parts.length == 2);
                
                DateTime begin;
                DateTime end;
                try {
                    begin = dateTimeParser.parseDateTime(parts[0]);
                    // alku oli aikaleima
                    try {
                        end = dateTimeParser.parseDateTime(parts[1]);
                    } catch (IllegalArgumentException e) {
                        // loppu ei ollut aikaleima, kokeillaan onko duration
                        try {
                            end = begin.plus((Duration)converters().get(Duration.class).convert(parts[1]));
                        } catch (InvalidValueException e1) {
                            // loppu ei ollut duration, oletetaan että oli period
                            end = begin.plus((Period)converters().get(Period.class).convert(parts[1]));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // alku ei ollut aikaleima, oletetaan että loppu on aikaleima
                    end = dateTimeParser.parseDateTime(parts[1]);
                    try {
                        // kokeillaan onko alku duration
                        begin = end.minus((Duration)converters().get(Duration.class).convert(parts[0]));
                    } catch (InvalidValueException e1) {
                        // alku ei ollut duration, oletetaan että oli period
                        begin = end.minus((Period)converters().get(Period.class).convert(parts[0]));
                    }
                }
                
                Assert.equal(begin.getZone(), end.getZone());
                Assert.equal(begin.getZone(), DateTimeZone.UTC);
                
                ret = new Interval(begin, end);
            } catch (RuntimeException e) {
                throw new InvalidValueException("interval", source);
            }
            
            if (!VALID.contains(ret)) {
                throw new IntervalNotWithinLimitsException(dateTimeParser.print(VALID.getStart()), dateTimeParser.print(VALID.getEnd()));
            }
            
            return ret;
        }
    };
    
    private final Converter<String,Duration> kesto = new Converter<String, Duration>() {
        @Override
        public Duration convert(String source) throws InvalidValueException {
            Duration ret;
            try {
                ret = Duration.parse(source);
            } catch (Exception e) {
                try {
                    ret = Period.parse(source).toStandardDuration();
                } catch (Exception e1) {
                    throw new InvalidValueException("duration", source);
                }
            }
            return ret;
        }
    };
    
    private final Converter<String,Period> jakso = new Converter<String, Period>() {
        @Override
        public Period convert(String source) throws InvalidValueException {
            Period ret;
            try {
                ret = Period.parse(source);
            } catch (Exception e) {
                throw new InvalidValueException("period", source);
            }
            return ret;
        }
    };

    public static final DateTimeFormatter dateTimeParser = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();
    public static final Interval VALID = new Interval(dateTimeParser.parseDateTime("2010-01-01T00:00:00Z"), dateTimeParser.parseDateTime("2030-01-01T00:00:00Z"));
    
    private final Converter<String,LocalDate> paiva = new Converter<String, LocalDate>() {
        private final DateTimeFormatter localDateParser = ISODateTimeFormat.localDateParser();
        
        private final LocalDate VALID_BEGIN = VALID.getStart().toLocalDate();
        private final LocalDate VALID_END = VALID.getEnd().toLocalDate();
        
        @Override
        public LocalDate convert(String source) throws InvalidValueException, LocalDateNotWithinLimitsException {
            try {
                LocalDate ret = localDateParser.parseLocalDate(source);
                if (ret.isBefore(VALID_BEGIN) || ret.isAfter(VALID_END)) {
                    throw new LocalDateNotWithinLimitsException(localDateParser.print(VALID_BEGIN), localDateParser.print(VALID_END));
                }
                return ret;
            } catch (RuntimeException e) {
                throw new InvalidValueException("date", source);
            }
        }
    };
    
    private final Converter<String,LocalTime> kellonaika = new Converter<String, LocalTime>() {
        private final DateTimeFormatter localTimeParser = ISODateTimeFormat.localTimeParser();
        
        @Override
        public LocalTime convert(String source) throws InvalidValueException, LocalDateNotWithinLimitsException {
            try {
                return localTimeParser.parseLocalTime(source);
            } catch (RuntimeException e) {
                throw new InvalidValueException("time", source);
            }
        }
    };
    
    private final Converter<String,DateTimeZone> zone = new Converter<String, DateTimeZone>() {
        @Override
        public DateTimeZone convert(String source) throws InvalidTimeZoneException {
            try {
                return DateTimeZone.forID(source);
            } catch (RuntimeException e) {
                throw new InvalidTimeZoneException(source);
            }
        }
    };
    
    private final Converter<String,URI> uri = new Converter<String, URI>() {
        @Override
        public URI convert(String source) throws InvalidValueException {
            try {
                return URI.create(source);
            } catch (RuntimeException e) {
                throw new InvalidValueException("uri", source);
            }
        }
    };
    
    
    
    
    
    public Map<Class<?>,Converter<String,?>> converters() { return newMap(
        Pair.of(Revision.class, revision),
        Pair.of(PropertyName.class, propertyName),
        Pair.of(Filters.class, filter),
        Pair.of(StartIndex.class, startIndex),
        Pair.of(SRSName.class, srsName),
        Pair.of(Count.class, count),
        
        Pair.of(URI.class, uri),
        Pair.of(LocalDate.class, paiva),
        Pair.of(LocalTime.class, kellonaika),
        Pair.of(DateTime.class, ajanhetki),
        Pair.of(Interval.class, interval),
        Pair.of(Duration.class, kesto),
        Pair.of(Period.class, jakso),
        Pair.of(DateTimeZone.class, zone),
        
        Pair.of(Boolean.class, bool),
        Pair.of(Short.class, _short),
        Pair.of(Integer.class, _int),
        Pair.of(Long.class, _long),
        Pair.of(BigDecimal.class, bigdecimal),
        Pair.of(BigInteger.class, biginteger),
        Pair.of(Character.class, character)
    );
    }
}
