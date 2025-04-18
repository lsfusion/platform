package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientWebAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.serializeObject;

public class InternalClientAction extends CallAction {

    private final boolean syncType;

    public InternalClientAction(ImList<Type> params, ImList<LP> targetPropList, boolean syncType) {
        super(1,  params, targetPropList);
        this.syncType = syncType;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ArrayList<byte[]> values = new ArrayList<>();
        ArrayList<byte[]> types = new ArrayList<>();
        byte[] returnType;

        LP targetProp = targetPropList.isEmpty() ? null : targetPropList.get(0);

        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        String exec = (String) context.getKeyObject(orderInterfaces.get(0));

        boolean remove = false;
        Matcher commandMatcher = Pattern.compile("remove (.*)").matcher(exec);
        if (commandMatcher.matches()) {
            remove = true;
            exec = commandMatcher.group(1);
        }

        if(!exec.contains(".") && exec.contains("(")) { //backward compatibility
           exec = exec.substring(0, exec.indexOf("("));
        }

        boolean isFile = !BaseUtils.isSimpleWord(exec); // not function
        try {
            for (int i = 1; i < orderInterfaces.size(); i++) {
                PropertyInterface orderInterface = orderInterfaces.get(i);
                ObjectValue objectValue = context.getKeys().get(orderInterface);
                values.add(FormChanges.serializeConvertFileValue(objectValue.getValue(), context));
                types.add(TypeSerializer.serializeType(objectValue.getType()));
            }
            returnType = targetProp != null ? TypeSerializer.serializeType(targetProp.property.getType()) : null;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        Object resource = exec;
        String resourceName = exec;
        if(isFile) {
            Result<String> fullPath = new Result<>();
            RawFileData fileData = ResourceUtils.findResourceAsFileData(exec, true, true, fullPath, "web");
            if(fileData != null) {
                fileData.getID(); // to calculate the cache

                resource = fileData;
                resourceName = fullPath.result;
            } else // we don't convert in EXTERNAL HTTP, so we won't do it here, because it's not clear what to do with the query encoding
                resource = FormChanges.convertFileValue(exec, context.getRemoteContext());
        }

        ClientWebAction clientWebAction = new ClientWebAction(resource, resourceName, exec, isFile, values, types, returnType, syncType, remove);
        if (syncType) {
            Object result = context.requestUserInteraction(clientWebAction);
            if(targetProp != null)
                targetProp.change(result, context);
        } else
            context.delayUserInteraction(clientWebAction);

        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if (type == ChangeFlowType.INTERNALASYNC || type == ChangeFlowType.INTERACTIVEWAIT || type == ChangeFlowType.INTERACTIVEAPI)
            return !syncType;

        return super.hasFlow(type, recursiveAbstracts);
    }
}