package lsfusion.server.logics.action;

import lsfusion.base.ResourceUtils;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.InitJSClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InitJSAction extends SystemAction {

    private List<LP> params;
    public InitJSAction(List<Object> params) {
        super(LocalizedString.create("InitJS"), SetFact.singletonOrder(new PropertyInterface()));
        this.params = params.stream().filter(param -> param instanceof LP).map(param -> (LP)param).collect(Collectors.toList());
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        List<String> resources = new ArrayList<>();

        for (LP param : params) {
            resources.addAll(ResourceUtils.getResources(Pattern.compile("/web/.*/" + ((String)param.read(context)).trim())));
        }

        context.requestUserInteraction(new InitJSClientAction(resources));
        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}