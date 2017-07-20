package lsfusion.server.lib;

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ClientActionLogger {

    static Logger logger;
    static {
        try {
            logger = Logger.getLogger("clientActionLog");
            logger.setLevel(Level.INFO);
            FileAppender fileAppender = new FileAppender(new EnhancedPatternLayout("%d{DATE} %5p %c{1} - %m%n%throwable{1000}"),
                    "logs/clientAction.log");
            logger.removeAllAppenders();
            logger.addAppender(fileAppender);

        } catch (Exception ignored) {
        }
    }
}
