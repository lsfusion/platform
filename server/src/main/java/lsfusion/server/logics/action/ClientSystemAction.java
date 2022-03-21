package lsfusion.server.logics.action;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.ClientJSAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.serializeObject;

public class ClientSystemAction extends SystemAction {

    private final String resourceName;
    private final boolean isFile;

    public ClientSystemAction(String resourceName, int size, boolean isFile) {
        super(LocalizedString.create("ClientJS"), SetFact.toOrderExclSet(size, i -> new PropertyInterface()));
        this.resourceName = resourceName;
        this.isFile = isFile;
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

        List<String> resources = ResourceUtils.getResources(Pattern.compile("/web/.*" + resourceName.trim()));
        String resource = isFile ? (resources.size() == 1 ? resources.get(0) : null) : resourceName;

        context.delayUserInteraction(new ClientJSAction(resource, resourceName, values, types, isFile, SystemProperties.inDevMode));

        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}