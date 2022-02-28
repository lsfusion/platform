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
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.serializeObject;

public class ClientSystemAction extends SystemAction {

    private final String js;

    public ClientSystemAction(String js, int size) {
        super(LocalizedString.create("ClientJS"), SetFact.toOrderExclSet(size, i -> new PropertyInterface()));
        this.js = js;
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

        context.delayUserInteraction(new ClientJSAction(js.contains(".js") || js.contains(".css") ? ResourceUtils.getResources(Pattern.compile("/web/.*/" + js.trim())) : Collections.singletonList(js),
                values, types));

        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}