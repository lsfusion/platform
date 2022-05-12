package lsfusion.server.logics.action;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientWebAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.serializeObject;

public class WebClientAction extends SystemAction {

    private final Object resource;
    private final String resourceName;
    private final boolean isFile;
    private final boolean syncType;

    public WebClientAction(String resourceName, int size, boolean isFile, boolean syncType) {
        super(LocalizedString.create("Web client"), SetFact.toOrderExclSet(size, i -> new PropertyInterface()));
        this.isFile = isFile;
        this.syncType = syncType;

        if(isFile) {
            Result<String> fullPath = new Result<>();
            RawFileData fileData = ResourceUtils.findResourceAsFileData(resourceName, false, false, fullPath, "web");
            fileData.getID(); // to calculate the cache
            resource = fileData;
            this.resourceName = fullPath.result;
        } else {
            resource = resourceName;
            this.resourceName = resourceName;
        }
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ArrayList<byte[]> values = new ArrayList<>();
        ArrayList<byte[]> types = new ArrayList<>();

        try {
            for (PropertyInterface orderInterface : getOrderInterfaces()) {
                ObjectValue objectValue = context.getKeys().get(orderInterface);
                values.add(serializeObject(objectValue.getValue()));
                types.add(TypeSerializer.serializeType(objectValue.getType()));
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        ClientWebAction clientWebAction = new ClientWebAction(resource, resourceName, values, types, isFile, syncType);
        if (syncType)
            context.requestUserInteraction(clientWebAction);
        else
            context.delayUserInteraction(clientWebAction);

        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}