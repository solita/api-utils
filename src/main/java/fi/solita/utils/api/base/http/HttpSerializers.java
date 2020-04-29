package fi.solita.utils.api.base.http;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.find;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.equalTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.convert.converter.Converter;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.filtering.Filter;
import fi.solita.utils.api.filtering.FilterParser;
import fi.solita.utils.api.types.Count;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
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
    
    public static final class InvalidEnumException extends RuntimeException {
        public final String value;
        public final String name;
        public final List<String> validValues;
        public <E extends Enum<E>> InvalidEnumException(String source, Class<E> enumClass, Apply<E,String> serialization) {
            super(source);
            this.value = source;
            this.name = enumClass.getName();
            this.validValues = newList(map(serialization, enumClass.getEnumConstants()));
        }
    }
    
    protected static final class EnumConverter<E extends Enum<E>> implements Converter<String, E> {
        private final Class<E> enumClass;
        private final Apply<E, String> serialization;

        public static final <E extends Enum<E>> EnumConverter<E> of(Class<E> enumClass, Apply<E,String> serialization) {
            return new EnumConverter<E>(enumClass, serialization);
        }
        
        private EnumConverter(Class<E> enumClass, Apply<E,String> serialization) {
            this.enumClass = enumClass;
            this.serialization = serialization;
        }
        
        @Override
        public E convert(String source) {
            for (E v: enumClass.getEnumConstants()) {
                if (serialization.apply(v).equals(source)) {
                    return v;
                }
            }
            throw new InvalidEnumException(source, enumClass, serialization);
        }
    };

    public static final class InvalidRevisionException extends RuntimeException {
    }
    public static final class InvalidFilterException extends RuntimeException {
        public final List<String> validValues;
        public InvalidFilterException(List<String> validValues) {
            this.validValues = validValues;
        }
    }
    public static final class InvalidCountException extends RuntimeException {
        public final List<Integer> validValues;
        public InvalidCountException(List<Integer> validValues) {
            this.validValues = validValues;
        }
    }
    
    public static final class InvalidStartIndexException extends RuntimeException {
    }
    
    public static final class InvalidSRSNameException extends RuntimeException {
        public final List<String> validValues;
        public InvalidSRSNameException(List<String> validValues) {
            this.validValues = validValues;
        }
    }
    
    public static final class InvalidIntervalException extends RuntimeException {}
    public static final class BeginAndEndMustMatchException extends RuntimeException {}
    public static final class IntervalNotWithinLimitsException extends RuntimeException {
        public final String validStart;
        public final String validEnd;
        public IntervalNotWithinLimitsException(String validStart, String validEnd) {
            this.validStart = validStart;
            this.validEnd = validEnd;
        }
    }
    
    public static class InvalidLocalDateException extends RuntimeException {
        public final String localdate;
        public InvalidLocalDateException(String localdate) {
            super(localdate);
            this.localdate = localdate;
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
    
    public static class InvalidLocalTimeException extends RuntimeException {
        public final String localtime;
        public InvalidLocalTimeException(String localtime) {
            super(localtime);
            this.localtime = localtime;
        }
    }
    public static class InvalidDurationException extends RuntimeException {
        public final String duration;
        public InvalidDurationException(String duration) {
            super(duration);
            this.duration = duration;
        }
    }
    public static class InvalidTimeZoneException extends RuntimeException {
        public final String timeZone;
        public InvalidTimeZoneException(String timeZone) {
            super(timeZone);
            this.timeZone = timeZone;
        }
    }
    
    public static class InvalidDateTimeException extends RuntimeException {
        public final String datetime;
        public InvalidDateTimeException(String datetime) {
            super(datetime);
            this.datetime = datetime;
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
    
    public static class InvalidURIException extends RuntimeException {
        public final String uri;
        public InvalidURIException(String uri) {
            super(uri);
            this.uri = uri;
        }
    }
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Revision>> revision = Pair.of(Revision.class, new Converter<String, Revision>() {
        @Override
        public Revision convert(String source) throws InvalidRevisionException {
            try {
                long val = Long.parseLong(source);
                return new Revision(val);
            } catch (RuntimeException e) {
                throw new InvalidRevisionException();
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,PropertyName>> propertyName = Pair.of(PropertyName.class, new Converter<String, PropertyName>() {
        @Override
        public PropertyName convert(String source) {
            return new PropertyName(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Filters>> filter = Pair.of(Filters.class, new Converter<String, Filters>() {
        @Override
        public Filters convert(String source) {
            List<Filter> filters = FilterParser.parse(source);
            if (!filters.isEmpty()) {
                return new Filters(filters);
            }
            throw new InvalidFilterException(Filters.SUPPORTED_OPERATIONS);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Boolean>> bool = Pair.of(Boolean.class, new Converter<String, Boolean>() {
        @Override
        public Boolean convert(String source) {
            return Boolean.parseBoolean(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Character>> character = Pair.of(Character.class, new Converter<String, Character>() {
        @Override
        public Character convert(String source) {
            Assert.True(source.length() == 1);
            return source.charAt(0);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Short>> _short = Pair.of(Short.class, new Converter<String, Short>() {
        @Override
        public Short convert(String source) {
            return Short.parseShort(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Integer>> _int = Pair.of(Integer.class, new Converter<String, Integer>() {
        @Override
        public Integer convert(String source) {
            return Integer.parseInt(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Long>> _long = Pair.of(Long.class, new Converter<String, Long>() {
        @Override
        public Long convert(String source) {
            return Long.parseLong(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,BigInteger>> biginteger = Pair.of(BigInteger.class, new Converter<String, BigInteger>() {
        @Override
        public BigInteger convert(String source) {
            return new BigInteger(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,BigDecimal>> bigdecimal = Pair.of(BigDecimal.class, new Converter<String, BigDecimal>() {
        @Override
        public BigDecimal convert(String source) {
            return new BigDecimal(source);
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Count>> count = Pair.of(Count.class, new Converter<String, Count>() {
        @Override
        public Count convert(String source) throws InvalidCountException {
            try {
                int val = Integer.parseInt(source);
                Assert.True(Count.validValues.contains(val));
                return new Count(val);
            } catch (RuntimeException e) {
                throw new InvalidCountException(Count.validValues);
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,StartIndex>> startIndex = Pair.of(StartIndex.class, new Converter<String, StartIndex>() {
        @Override
        public StartIndex convert(String source) throws InvalidStartIndexException {
            try {
                int val = Integer.parseInt(source);
                return new StartIndex(val);
            } catch (RuntimeException e) {
                throw new InvalidStartIndexException();
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,SRSName>> srsName = Pair.of(SRSName.class, new Converter<String, SRSName>() {
        @Override
        public SRSName convert(String source) throws InvalidSRSNameException {
            try {
                Option<SRSName> found = find(SRSName_.value.andThen(equalTo(source)), SRSName.validValues);
                Assert.defined(found);
                return found.get();
            } catch (RuntimeException e) {
                throw new InvalidSRSNameException(newList(map(SRSName_.value, SRSName.validValues)));
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,DateTime>> ajanhetki = Pair.of(DateTime.class, new Converter<String, DateTime>() {
        private final DateTime VALID_BEGIN = VALID.getStart();
        private final DateTime VALID_END = VALID.getEnd();
        
        @Override
        public DateTime convert(String source) throws InvalidDateTimeException, DateTimeNotWithinLimitsException {
            DateTime ret;
            try {
                ret = dateTimeParser.parseDateTime(source);
            } catch (Exception e) {
                throw new InvalidDateTimeException(source);
            }
            
            if (ret.isBefore(VALID_BEGIN) || ret.isAfter(VALID_END)) {
                throw new IntervalNotWithinLimitsException(dateTimeParser.print(VALID_BEGIN), dateTimeParser.print(VALID_END));
            }
            
            return ret;
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,Duration>> kesto = Pair.of(Duration.class, new Converter<String, Duration>() {
        @Override
        public Duration convert(String source) throws InvalidDurationException {
            Duration ret;
            try {
                ret = Duration.parse(source);
            } catch (Exception e) {
                throw new InvalidDurationException(source);
            }
            return ret;
        }
    });

    public static final DateTimeFormatter dateTimeParser = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();
    public static final Interval VALID = new Interval(dateTimeParser.parseDateTime("2010-01-01T00:00:00Z"), dateTimeParser.parseDateTime("2030-01-01T00:00:00Z"));
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,LocalDate>> paiva = Pair.of(LocalDate.class, new Converter<String, LocalDate>() {
        private final DateTimeFormatter localDateParser = ISODateTimeFormat.localDateParser();
        
        private final LocalDate VALID_BEGIN = VALID.getStart().toLocalDate();
        private final LocalDate VALID_END = VALID.getEnd().toLocalDate();
        
        @Override
        public LocalDate convert(String source) throws InvalidLocalDateException, LocalDateNotWithinLimitsException {
            try {
                LocalDate ret = localDateParser.parseLocalDate(source);
                if (ret.isBefore(VALID_BEGIN) || ret.isAfter(VALID_END)) {
                    throw new LocalDateNotWithinLimitsException(localDateParser.print(VALID_BEGIN), localDateParser.print(VALID_END));
                }
                return ret;
            } catch (RuntimeException e) {
                throw new InvalidLocalDateException(source);
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,LocalTime>> kellonaika = Pair.of(LocalTime.class, new Converter<String, LocalTime>() {
        private final DateTimeFormatter localTimeParser = ISODateTimeFormat.localTimeParser();
        
        @Override
        public LocalTime convert(String source) throws InvalidLocalDateException, LocalDateNotWithinLimitsException {
            try {
                return localTimeParser.parseLocalTime(source);
            } catch (RuntimeException e) {
                throw new InvalidLocalTimeException(source);
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,DateTimeZone>> zone = Pair.of(DateTimeZone.class, new Converter<String, DateTimeZone>() {
        @Override
        public DateTimeZone convert(String source) throws InvalidTimeZoneException {
            try {
                return DateTimeZone.forID(source);
            } catch (RuntimeException e) {
                throw new InvalidTimeZoneException(source);
            }
        }
    });
    
    public final Map.Entry<? extends Class<?>, ? extends Converter<String,URI>> uri = Pair.of(URI.class, new Converter<String, URI>() {
        @Override
        public URI convert(String source) throws InvalidURIException {
            try {
                return URI.create(source);
            } catch (RuntimeException e) {
                throw new InvalidURIException(source);
            }
        }
    });
    
    public Map<Class<?>,? extends Converter<?,?>> converters() { return newMap(
            revision,
            propertyName,
            filter,
            bool,
            character,
            _short,
            _int,
            _long,
            biginteger,
            bigdecimal,
            count,
            startIndex,
            srsName,
            ajanhetki,
            paiva,
            kellonaika,
            kesto,
            zone,
            uri
        );
    }
}
