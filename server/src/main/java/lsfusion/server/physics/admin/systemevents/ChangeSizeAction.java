package lsfusion.server.physics.admin.systemevents;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.ChangeSizeClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChangeSizeAction extends InternalAction {
    private final ClassPropertyInterface oldSizeInterface;
    private final ClassPropertyInterface newSizeInterface;

    public ChangeSizeAction(SystemEventsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        oldSizeInterface = i.next();
        newSizeInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        DataObject oldSizeObject = context.getDataKeyValue(oldSizeInterface);
        DataObject newSizeObject = context.getDataKeyValue(newSizeInterface);

        Map<String, FileData> resources = new HashMap<>();

        try {

            ConcreteCustomClass size = context.getBL().systemEventsLM.size;
            DataObject normal = size.getDataObject("normal");
            DataObject mini = size.getDataObject("mini");
            DataObject tiny = size.getDataObject("tiny");

            if (newSizeObject.equals(normal)) {
                if (oldSizeObject.equals(mini)) {
                    resources.put("mini-padding.css", null);
                    resources.put("mini-font.css", null);
                } else if (oldSizeObject.equals(tiny)) {
                    resources.put("tiny-padding.css", null);
                    resources.put("tiny-font.css", null);
                }
            } else if (newSizeObject.equals(mini)) {
                resources.put("mini-padding.css", readResource(context, "mini-padding.css"));
                resources.put("mini-font.css", readResource(context, "mini-font.css"));
                if (oldSizeObject.equals(tiny)) {
                    resources.put("tiny-padding.css", null);
                    resources.put("tiny-font.css", null);
                }
            } else if (newSizeObject.equals(tiny)) {
                resources.put("tiny-padding.css", readResource(context, "tiny-padding.css"));
                resources.put("tiny-font.css", readResource(context, "tiny-font.css"));
                if (oldSizeObject.equals(mini)) {
                    resources.put("mini-padding.css", null);
                    resources.put("mini-font.css", null);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }

        context.delayUserInteraction(new ChangeSizeClientAction(resources));
    }

    private FileData readResource(ExecutionContext context, String resource) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        context.getBL().utilsLM.findAction("readResource[STRING]").execute(context, new DataObject(resource));
        return (FileData) context.getBL().utilsLM.findProperty("resource[]").read(context);
    }
}