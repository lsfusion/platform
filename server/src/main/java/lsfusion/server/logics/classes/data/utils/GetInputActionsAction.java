package lsfusion.server.logics.classes.data.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInputListAction;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class GetInputActionsAction extends InternalAction {
    private final ClassPropertyInterface actionInterface;

    public GetInputActionsAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        actionInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            JSONArray resultActions = new JSONArray();

            JSONObject action = new JSONObject((String) context.getKeyValue(actionInterface).getValue());
            if(action.has("id")) {
                ImList<AsyncMapInputListAction<PropertyInterface>> inputActions = RemoteNavigator.getGlobalNotificationInputActions(action.optInt("id"));
                if (inputActions != null)
                    for(int i = 0, size = inputActions.size(); i < size; i++) {
                        AsyncMapInputListAction<PropertyInterface> inputAction = inputActions.get(i);

                        JSONObject resultAction = new JSONObject();
                        resultAction.put("action", inputAction.id);
                        resultAction.put("title", ""); // title is mandatory
                        resultAction.put("icon", AppServerImage.convertFileValue(inputAction.action.get(context.getRemoteContext()), false));

                        resultActions.put(resultAction);
                    }
            }

            findProperty("inputActions[]").change(resultActions.toString(), context);
        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

}