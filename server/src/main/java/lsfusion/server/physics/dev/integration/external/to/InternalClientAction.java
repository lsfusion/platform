package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientWebAction;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.serializeObject;

public class InternalClientAction extends CallAction {

    private final boolean syncType;
    @Deprecated
    private final String resourceName;

    public InternalClientAction(ImList<Type> params, ImList<LP> targetPropList, boolean syncType) {
        this(params, targetPropList, syncType, null, null);
    }

    public InternalClientAction(ImList<Type> params, ImList<LP> targetPropList, boolean syncType, String resourceName, ImList<Type> types) {
        super(resourceName != null ? 0 : 1,  resourceName != null ? types : params, targetPropList);
        this.syncType = syncType;
        this.resourceName = resourceName;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ArrayList<byte[]> values = new ArrayList<>();
        ArrayList<byte[]> types = new ArrayList<>();
        byte[] returnType;

        LP targetProp = targetPropList.isEmpty() ? null : targetPropList.get(0);

        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        String exec = resourceName != null ? resourceName : (String) context.getKeyObject(orderInterfaces.get(0));
        boolean isFile = exec.endsWith(".js") || exec.endsWith(".css");

        try {
            for (int i = resourceName != null ? 0 : 1; i < orderInterfaces.size(); i++) {
                PropertyInterface orderInterface = orderInterfaces.get(i);
                ObjectValue objectValue = context.getKeys().get(orderInterface);
                values.add(serializeObject(objectValue.getValue()));
                types.add(TypeSerializer.serializeType(objectValue.getType()));
            }
            returnType = targetProp != null ? TypeSerializer.serializeType(targetProp.property.getType()) : null;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        Object resource;
        String resourceName;
        if(isFile) {
            Pair<Object, String> resourceData = findResourceAsFileData(exec);
            resource = resourceData.first;
            resourceName = resourceData.second;
        } else {
            resource = exec;
            resourceName = exec;
        }

        ClientWebAction clientWebAction = new ClientWebAction(resource, resourceName, values, types, returnType, isFile, syncType);
        if (syncType) {
            Object result = context.requestUserInteraction(clientWebAction);
            if(targetProp != null)
                targetProp.change(result, context);
        } else
            context.delayUserInteraction(clientWebAction);

        return FlowResult.FINISH;
    }

    @IdentityLazy
    public Pair<Object, String> findResourceAsFileData(String exec) {
        Result<String> fullPath = new Result<>();
        RawFileData fileData = ResourceUtils.findResourceAsFileData(exec, false, false, fullPath, "web");
        fileData.getID(); // to calculate the cache
        return Pair.create(fileData, fullPath.result);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}