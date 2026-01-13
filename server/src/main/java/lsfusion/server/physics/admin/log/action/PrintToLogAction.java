package lsfusion.server.physics.admin.log.action;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.log4j.*;

import static lsfusion.base.BaseUtils.nvl;

public class PrintToLogAction extends InternalAction {
    public PrintToLogAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String message = (String) getParamValue(0, context).getValue();
        String name = nvl((String) getParamValue(1, context).getValue(), "system");
        String level = nvl(((String) getParamValue(2, context).getValue()), "info");

        String loggerName = BaseUtils.capitalize(name) + "Logger";
        Logger logger = LogManager.exists(loggerName);
        if(logger == null) {
            logger = LogManager.getLogger(loggerName);
            FileAppender fa = new FileAppender();
            fa.setName(loggerName);
            fa.setFile("logs/" + name + ".log");
            fa.setLayout(new EnhancedPatternLayout("%d{DATE} %5p %c{1} - %m%n%throwable{1000}"));
            fa.setAppend(true);
            fa.activateOptions();
            logger.setLevel(Level.ALL);
            logger.addAppender(fa);
        }

        switch (level) {
            case "error":
                logger.error(message);
                break;
            case "warn":
                logger.warn(message);
                break;
            case "info":
            default:
                logger.info(message);
                break;
        }
    }
}