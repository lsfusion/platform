package lsfusion.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimpleFormatter extends Formatter {
    private String lineSeparator = System.getProperty("line.separator");

    public synchronized String format(LogRecord record) {
	StringBuffer sb = new StringBuffer();

	if (record.getSourceClassName() != null) {
	    sb.append(record.getSourceClassName());
	} else {
	    sb.append(record.getLoggerName());
	}
        sb.append(": ");

	String message = formatMessage(record);
	sb.append(record.getLevel().getLocalizedName());
	sb.append(": ");
	sb.append(message);
	sb.append(lineSeparator);
	if (record.getThrown() != null) {
	    try {
	        StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        record.getThrown().printStackTrace(pw);
	        pw.close();
		sb.append(sw.toString());
	    } catch (Exception ex) {
	    }
	}
	return sb.toString();
    }
}
