package lsfusion.server.data;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class SimpleLayout extends Layout {

    public String format(LoggingEvent event) {
        return event.getRenderedMessage();
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    public void activateOptions() {
    }
}
