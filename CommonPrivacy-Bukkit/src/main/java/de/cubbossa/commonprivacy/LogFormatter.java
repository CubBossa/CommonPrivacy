package de.cubbossa.commonprivacy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

  private final String format;

  public LogFormatter(String format) {
    this.format = format;
  }

  @Override
  public String format(LogRecord record) {
    ZonedDateTime zdt = ZonedDateTime.ofInstant(
        record.getInstant(), ZoneId.systemDefault());
    String source;
    if (record.getSourceClassName() != null) {
      source = record.getSourceClassName();
      if (record.getSourceMethodName() != null) {
        source += " " + record.getSourceMethodName();
      }
    } else {
      source = record.getLoggerName();
    }
    String message = formatMessage(record);
    String throwable = "";
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = sw.toString();
    }
    return String.format(format,
        zdt,
        source,
        record.getLoggerName(),
        record.getLevel().getName(),
        message,
        throwable);
  }
}
