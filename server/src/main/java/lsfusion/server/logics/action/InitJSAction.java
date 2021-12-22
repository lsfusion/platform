package lsfusion.server.logics.action;

import lsfusion.base.ResourceUtils;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.InitJSClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InitJSAction extends SystemAction {

    public InitJSAction() {
        super(LocalizedString.create("InitJS"), SetFact.singletonOrder(new PropertyInterface()));
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        List<String> resources = new ArrayList<>();
        for (String s : String.valueOf(context.getSingleKeyValue().getValue()).split(",")) {
            List<String> resourcesPaths = ResourceUtils.getResources(Pattern.compile("/web/.*/" + s.trim()));
            if (resourcesPaths.size() == 1)
                resources.add(resourcesPaths.get(0));
        }
        context.requestUserInteraction(new InitJSClientAction(resources));
        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}