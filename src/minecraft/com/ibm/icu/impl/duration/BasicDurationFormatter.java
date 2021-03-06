package com.ibm.icu.impl.duration;

import java.util.Date;
import java.util.TimeZone;
















class BasicDurationFormatter
  implements DurationFormatter
{
  private PeriodFormatter formatter;
  private PeriodBuilder builder;
  private DateFormatter fallback;
  private long fallbackLimit;
  private String localeName;
  private TimeZone timeZone;
  
  public BasicDurationFormatter(PeriodFormatter formatter, PeriodBuilder builder, DateFormatter fallback, long fallbackLimit)
  {
    this.formatter = formatter;
    this.builder = builder;
    this.fallback = fallback;
    this.fallbackLimit = (fallbackLimit < 0L ? 0L : fallbackLimit);
  }
  




  protected BasicDurationFormatter(PeriodFormatter formatter, PeriodBuilder builder, DateFormatter fallback, long fallbackLimit, String localeName, TimeZone timeZone)
  {
    this.formatter = formatter;
    this.builder = builder;
    this.fallback = fallback;
    this.fallbackLimit = fallbackLimit;
    this.localeName = localeName;
    this.timeZone = timeZone;
  }
  
  public String formatDurationFromNowTo(Date targetDate) {
    long now = System.currentTimeMillis();
    long duration = now - targetDate.getTime();
    return formatDurationFrom(duration, now);
  }
  
  public String formatDurationFromNow(long duration) {
    return formatDurationFrom(duration, System.currentTimeMillis());
  }
  
  public String formatDurationFrom(long duration, long referenceDate) {
    String s = doFallback(duration, referenceDate);
    if (s == null) {
      Period p = doBuild(duration, referenceDate);
      s = doFormat(p);
    }
    return s;
  }
  
  public DurationFormatter withLocale(String locName) {
    if (!locName.equals(localeName)) {
      PeriodFormatter newFormatter = formatter.withLocale(locName);
      PeriodBuilder newBuilder = builder.withLocale(locName);
      DateFormatter newFallback = fallback == null ? null : fallback.withLocale(locName);
      

      return new BasicDurationFormatter(newFormatter, newBuilder, newFallback, fallbackLimit, locName, timeZone);
    }
    

    return this;
  }
  
  public DurationFormatter withTimeZone(TimeZone tz) {
    if (!tz.equals(timeZone)) {
      PeriodBuilder newBuilder = builder.withTimeZone(tz);
      DateFormatter newFallback = fallback == null ? null : fallback.withTimeZone(tz);
      

      return new BasicDurationFormatter(formatter, newBuilder, newFallback, fallbackLimit, localeName, tz);
    }
    

    return this;
  }
  
  protected String doFallback(long duration, long referenceDate) {
    if ((fallback != null) && (fallbackLimit > 0L) && (Math.abs(duration) >= fallbackLimit))
    {

      return fallback.format(referenceDate + duration);
    }
    return null;
  }
  
  protected Period doBuild(long duration, long referenceDate) {
    return builder.createWithReferenceDate(duration, referenceDate);
  }
  
  protected String doFormat(Period period) {
    if (!period.isSet()) {
      throw new IllegalArgumentException("period is not set");
    }
    return formatter.format(period);
  }
}
