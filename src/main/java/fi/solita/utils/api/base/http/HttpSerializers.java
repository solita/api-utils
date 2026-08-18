package fi.solita.utils.api.base.http;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.find;
import static fi.solita.utils.functional.Functional.map;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

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
import java.util.Optional;
import fi.solita.utils.functional.Pair;

public class HttpSerializers {
    
    @SuppressWarnings("unused")
    private final Serializers s;

    public HttpSerializers(Serializers s) {
        this.s = s;
    }
    
    
    
    public static final <E extends Enum<E>> Function<String, E> enumConverter(final Class<E> enumClass, final Function<E,String> serialization) {
        return new Function<String, E>() {
            @Override
            public E apply(String source) {
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
    
    public static final class ExpectedSingletonException extends RuntimeException {
        public ExpectedSingletonException() {
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
    
    private final Function<String,Revision> revision = new Function<String, Revision>() {
        @Override
        public Revision apply(String source) throws InvalidValueException {
            try {
                long val = Long.parseLong(source);
                return new Revision(val);
            } catch (RuntimeException e) {
                throw new InvalidValueException("revision", source);
            }
        }
    };
    
    private final Function<String,PropertyName> propertyName = PropertyName_.of;
    
    private final Function<String,Filters> filter = new Function<String, Filters>() {
        @Override
        public Filters apply(String source) {
            List<List<Filter>> filters = FilterParser.parse(source);
            if (!filters.isEmpty()) {
                return new Filters(filters);
            }
            throw new InvalidFilterException(Filters.SUPPORTED_OPERATIONS);
        }
    };
    
    private final Function<String,Boolean> bool = new Function<String, Boolean>() {
        @Override
        public Boolean apply(String source) {
            return Boolean.parseBoolean(source);
        }
    };
    
    private final Function<String,Character> character = new Function<String, Character>() {
        @Override
        public Character apply(String source) {
            Assert.True(source.length() == 1);
            return source.charAt(0);
        }
    };
    
    private final Function<String,Short> _short = new Function<String, Short>() {
        @Override
        public Short apply(String source) {
            return Short.parseShort(source);
        }
    };
    
    private final Function<String,Integer> _int = new Function<String, Integer>() {
        @Override
        public Integer apply(String source) {
            return Integer.parseInt(source);
        }
    };
    
    private final Function<String,Long> _long = new Function<String, Long>() {
        @Override
        public Long apply(String source) {
            return Long.parseLong(source);
        }
    };
    
    private final Function<String,Double> _double = new Function<String, Double>() {
        @Override
        public Double apply(String source) {
            return Double.parseDouble(source);
        }
    };
    
    private final Function<String,BigInteger> biginteger = new Function<String, BigInteger>() {
        @Override
        public BigInteger apply(String source) {
            return new BigInteger(source);
        }
    };
    
    private final Function<String,BigDecimal> bigdecimal = new Function<String, BigDecimal>() {
        @Override
        public BigDecimal apply(String source) {
            return new BigDecimal(source);
        }
    };
    
    private final Function<String,Count> count = new Function<String, Count>() {
        @Override
        public Count apply(String source) throws InvalidValueException {
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
    
    private final Function<String,StartIndex> startIndex = new Function<String, StartIndex>() {
        @Override
        public StartIndex apply(String source) throws InvalidStartIndexException {
            try {
                int val = Integer.parseInt(source);
                return new StartIndex(val);
            } catch (RuntimeException e) {
                throw new InvalidStartIndexException();
            }
        }
    };
    
    private final Function<String,SRSName> srsName = new Function<String, SRSName>() {
        @Override
        public SRSName apply(String source) throws InvalidValueException {
            try {
                Optional<SRSName> found = find(x -> x.value.equals(source), SRSName.validValues);
                Assert.defined(found);
                return found.get();
            } catch (RuntimeException e) {
                throw new InvalidValueException("srs", source, newList(map(SRSName_.value, SRSName.validValues)));
            }
        }
    };
    
    private final Function<String,DateTime> ajanhetki = new Function<String, DateTime>() {
        private final DateTime VALID_BEGIN = VALID.getStart();
        private final DateTime VALID_END = VALID.getEnd();
        
        @Override
        public DateTime apply(String source) throws InvalidValueException, DateTimeNotWithinLimitsException {
            DateTime ret;
            try {
                ret = dateTimeParser.parseDateTime(source);
            } catch (Exception e) {
                try {
                    // some leniency: accept an interval of 0 length as an instant
                    Interval asInterval = interval.apply(source);
                    Assert.equal(asInterval.getStart(), asInterval.getEnd());
                    ret = asInterval.getStart();
                } catch (Exception e1) {
                    throw new InvalidValueException("datetime", source);
                }
            }
            
            if (ret.isBefore(VALID_BEGIN) || ret.isAfter(VALID_END)) {
                throw new IntervalNotWithinLimitsException(dateTimeParser.print(VALID_BEGIN), dateTimeParser.print(VALID_END));
            }
            
            return ret;
        }
    };
    
    private final Function<String,Interval> interval = new Function<String, Interval>() {
        @Override
        public Interval apply(String source) throws InvalidValueException, IntervalNotWithinLimitsException {
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
                            end = begin.plus((Duration)converters().get(Duration.class).apply(parts[1]));
                        } catch (InvalidValueException e1) {
                            // loppu ei ollut duration, oletetaan että oli period
                            end = begin.plus((Period)converters().get(Period.class).apply(parts[1]));
                        }
                        if (end.isAfter(VALID.getEnd())) {
                            end = VALID.getEnd();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // alku ei ollut aikaleima, oletetaan että loppu on aikaleima
                    end = dateTimeParser.parseDateTime(parts[1]);
                    try {
                        // kokeillaan onko alku duration
                        begin = end.minus((Duration)converters().get(Duration.class).apply(parts[0]));
                    } catch (InvalidValueException e1) {
                        // alku ei ollut duration, oletetaan että oli period
                        begin = end.minus((Period)converters().get(Period.class).apply(parts[0]));
                    }
                    if (begin.isBefore(VALID.getStart())) {
                        begin = VALID.getStart();
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
    
    private final Function<String,Duration> kesto = new Function<String, Duration>() {
        @Override
        public Duration apply(String source) throws InvalidValueException {
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
    
    private final Function<String,Period> jakso = new Function<String, Period>() {
        @Override
        public Period apply(String source) throws InvalidValueException {
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
    
    private final Function<String,LocalDate> paiva = new Function<String, LocalDate>() {
        private final DateTimeFormatter localDateParser = ISODateTimeFormat.localDateParser();
        
        private final LocalDate VALID_BEGIN = VALID.getStart().toLocalDate();
        private final LocalDate VALID_END = VALID.getEnd().toLocalDate();
        
        @Override
        public LocalDate apply(String source) throws InvalidValueException, LocalDateNotWithinLimitsException {
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
    
    private final Function<String,LocalTime> kellonaika = new Function<String, LocalTime>() {
        private final DateTimeFormatter localTimeParser = ISODateTimeFormat.localTimeParser();
        
        @Override
        public LocalTime apply(String source) throws InvalidValueException, LocalDateNotWithinLimitsException {
            try {
                return localTimeParser.parseLocalTime(source);
            } catch (RuntimeException e) {
                throw new InvalidValueException("time", source);
            }
        }
    };
    
    private final Function<String,DateTimeZone> zone = new Function<String, DateTimeZone>() {
        @Override
        public DateTimeZone apply(String source) throws InvalidTimeZoneException {
            try {
                return DateTimeZone.forID(source);
            } catch (RuntimeException e) {
                throw new InvalidTimeZoneException(source);
            }
        }
    };
    
    private final Function<String,URI> uri = new Function<String, URI>() {
        @Override
        public URI apply(String source) throws InvalidValueException {
            try {
                return URI.create(source);
            } catch (RuntimeException e) {
                throw new InvalidValueException("uri", source);
            }
        }
    };
    
    private final Function<String,UUID> uuid = new Function<String, UUID>() {
        @Override
        public UUID apply(String source) throws InvalidValueException {
            try {
                return UUID.fromString(source);
            } catch (RuntimeException e) {
                throw new InvalidValueException("uuid", source);
            }
        }
    };
    
    
    
    public Map<Class<?>,Function<String,?>> converters() { return newMap(
        Pair.of(Revision.class, revision),
        Pair.of(PropertyName.class, propertyName),
        Pair.of(Filters.class, filter),
        Pair.of(StartIndex.class, startIndex),
        Pair.of(SRSName.class, srsName),
        Pair.of(Count.class, count),
        
        Pair.of(URI.class, uri),
        Pair.of(UUID.class, uuid),
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
        Pair.of(Double.class, _double),
        Pair.of(BigDecimal.class, bigdecimal),
        Pair.of(BigInteger.class, biginteger),
        Pair.of(Character.class, character)
    );
    }
}
