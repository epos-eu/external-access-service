package org.epos.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CustomInstantDeserializer<T extends Temporal> extends JsonDeserializer<T> {

  private final Class<T> valueType;
  private final DateTimeFormatter formatter;

  private final Function<TemporalAccessor, T> parsedToValue;
  private final Function<FromIntegerArguments, T> fromMilliseconds;
  private final Function<FromDecimalArguments, T> fromNanoseconds;
  private final BiFunction<T, ZoneId, T> adjust;

  public static final CustomInstantDeserializer<Instant> INSTANT = new CustomInstantDeserializer<>(
          Instant.class,
          DateTimeFormatter.ISO_INSTANT,
          Instant::from,
          args -> Instant.ofEpochMilli(args.value),
          args -> Instant.ofEpochSecond(args.integer, args.fraction),
          (d, z) -> d // Instant is always UTC, no adjustment
  );

  public static final CustomInstantDeserializer<OffsetDateTime> OFFSET_DATE_TIME = new CustomInstantDeserializer<>(
          OffsetDateTime.class,
          DateTimeFormatter.ISO_OFFSET_DATE_TIME,
          OffsetDateTime::from,
          args -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(args.value), args.zoneId),
          args -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(args.integer, args.fraction), args.zoneId),
          (d, z) -> d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime()))
  );

  public static final CustomInstantDeserializer<ZonedDateTime> ZONED_DATE_TIME = new CustomInstantDeserializer<>(
          ZonedDateTime.class,
          DateTimeFormatter.ISO_ZONED_DATE_TIME,
          ZonedDateTime::from,
          args -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(args.value), args.zoneId),
          args -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(args.integer, args.fraction), args.zoneId),
          ZonedDateTime::withZoneSameInstant
  );

  public CustomInstantDeserializer(
          Class<T> valueType,
          DateTimeFormatter formatter,
          Function<TemporalAccessor, T> parsedToValue,
          Function<FromIntegerArguments, T> fromMilliseconds,
          Function<FromDecimalArguments, T> fromNanoseconds,
          BiFunction<T, ZoneId, T> adjust
  ) {
    this.valueType = valueType;
    this.formatter = formatter;
    this.parsedToValue = parsedToValue;
    this.fromMilliseconds = fromMilliseconds;
    this.fromNanoseconds = fromNanoseconds;
    this.adjust = adjust;
  }

  @Override
  public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonToken token = parser.getCurrentToken();

    switch (token) {
      case VALUE_NUMBER_FLOAT: {
        BigDecimal value = parser.getDecimalValue();
        long seconds = value.longValue();
        int nanoseconds = extractNanosecondDecimal(value, seconds);
        return fromNanoseconds.apply(new FromDecimalArguments(seconds, nanoseconds, getZone(context)));
      }

      case VALUE_NUMBER_INT: {
        long timestamp = parser.getLongValue();
        if (context.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
          return fromNanoseconds.apply(new FromDecimalArguments(timestamp, 0, getZone(context)));
        } else {
          return fromMilliseconds.apply(new FromIntegerArguments(timestamp, getZone(context)));
        }
      }

      case VALUE_STRING: {
        String string = parser.getText().trim();
        if (string.isEmpty()) {
          return null;
        }
        if (string.endsWith("+0000")) {
          string = string.substring(0, string.length() - 5) + "Z";
        }

        try {
          TemporalAccessor acc = formatter.parse(string);
          T value = parsedToValue.apply(acc);
          if (context.isEnabled(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)) {
            return adjust.apply(value, getZone(context));
          } else {
            return value;
          }
        } catch (DateTimeException e) {
          throw new IOException("Failed to deserialize " + valueType.getSimpleName() + ": " + string, e);
        }
      }

      default:
        throw new IOException("Unexpected token (" + token + ") when deserializing " + valueType.getSimpleName());
    }
  }

  private ZoneId getZone(DeserializationContext context) {
    if (valueType == Instant.class) {
      return null;
    } else {
      return DateTimeUtils.toZoneId(context.getTimeZone());
    }
  }

  private int extractNanosecondDecimal(BigDecimal value, long wholeSeconds) {
    BigDecimal fractional = value.subtract(BigDecimal.valueOf(wholeSeconds));
    BigDecimal nanos = fractional.movePointRight(9);
    return nanos.intValue();
  }

  private static class FromIntegerArguments {
    public final long value;
    public final ZoneId zoneId;

    public FromIntegerArguments(long value, ZoneId zoneId) {
      this.value = value;
      this.zoneId = zoneId;
    }
  }

  private static class FromDecimalArguments {
    public final long integer;
    public final int fraction;
    public final ZoneId zoneId;

    public FromDecimalArguments(long integer, int fraction, ZoneId zoneId) {
      this.integer = integer;
      this.fraction = fraction;
      this.zoneId = zoneId;
    }
  }
}
