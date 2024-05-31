package lsfusion.server.physics.admin.systemevents;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.LoadResourceClientAction;
import lsfusion.interop.action.UnloadResourceClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.nullEquals;

public class UpdateSizeAction extends InternalAction {
    private final ClassPropertyInterface oldSizeInterface;
    private final ClassPropertyInterface newSizeInterface;

    public UpdateSizeAction(SystemEventsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        oldSizeInterface = i.next();
        newSizeInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        DataObject oldSizeObject = context.getDataKeyValue(oldSizeInterface);
        DataObject newSizeObject = context.getDataKeyValue(newSizeInterface);

        Map<String, FileData> loadResources = new HashMap<>();
        List<String> unloadResources = new ArrayList<>();

        try {
            String oldPaddingCss = (String) context.getBL().systemEventsLM.paddingCss.read(context, oldSizeObject);
            String newPaddingCss = (String) context.getBL().systemEventsLM.paddingCss.read(context, newSizeObject);
            if(!nullEquals(oldPaddingCss, newPaddingCss)) {
                if(oldPaddingCss != null) {
                    unloadResources.add(oldPaddingCss);
                }
                if(newPaddingCss != null) {
                    loadResources.put(newPaddingCss, readResource(context, newPaddingCss));
                }
            }

            String oldFontCss = (String) context.getBL().systemEventsLM.fontCss.read(context, oldSizeObject);
            String newFontCss = (String) context.getBL().systemEventsLM.fontCss.read(context, newSizeObject);
            if(!nullEquals(oldFontCss, newFontCss)) {
                if(oldFontCss != null) {
                    unloadResources.add(oldFontCss);
                }
                if(newFontCss != null) {
                    loadResources.put(newFontCss, readResource(context, newFontCss));
                }
            }

            context.getBL().systemEventsLM.storeCurrentSize.execute(context);

        } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }

        for (Map.Entry<String, FileData> resource : loadResources.entrySet()) {
            context.delayUserInteraction(new LoadResourceClientAction(resource.getKey(), resource.getValue()));
        }

        for (String resource : unloadResources) {
            context.delayUserInteraction(new UnloadResourceClientAction(resource));
        }
    }

    private FileData readResource(ExecutionContext context, String resource) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        context.getBL().utilsLM.findAction("readResource[STRING]").execute(context, new DataObject(resource));
        return (FileData) context.getBL().utilsLM.findProperty("resource[]").read(context);
    }
}