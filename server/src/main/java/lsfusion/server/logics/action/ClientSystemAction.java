package lsfusion.server.logics.action;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.ClientJSAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.serializeObject;

public class ClientSystemAction extends SystemAction {

    private final List<String> jsList;

    public ClientSystemAction(List<String> jsList, int size) {
        super(LocalizedString.create("ClientJS"), SetFact.toOrderExclSet(size, i -> new PropertyInterface()));
        this.jsList = jsList;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        List<String> resources = new ArrayList<>();
        if (jsList.size() == 1 && !jsList.get(0).contains(".js"))
            resources.addAll(jsList);
        else
            jsList.forEach(js -> resources.addAll(ResourceUtils.getResources(Pattern.compile("/web/.*/" + js.trim()))));

        ArrayList<byte[]> keys = new ArrayList<>();
        for (ObjectValue value : context.getKeys().values()) {
            try {
                keys.add(serializeObject(value.getValue()));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        context.requestUserInteraction(new ClientJSAction(resources, keys));
        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}