package lsfusion.server.physics.dev.debug.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.event.LinkType;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.sql.SQLException;

public class ShowRecDepAction extends SystemExplicitAction {

    private final boolean showRec;
    private final ImSet<ActionOrProperty> props;
    private final boolean global;

    public ShowRecDepAction(boolean showRec, ImSet<ActionOrProperty> props, boolean global) {
        this.showRec = showRec;
        this.props = props;
        this.global = global;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        BusinessLogics bl = context.getBL();
        if(showRec) {
            for(ActionOrProperty actionOrProp : bl.getOrderActionOrProperties())
                if (actionOrProp instanceof Action)
                    ((Action)actionOrProp).showRec = props.contains(actionOrProp);
        } else {
            try {
                bl.LM.findProperty("showResult[]").change(bl.buildShowDeps(props, LinkType.RECUSED, global ? ApplyFilter.NO : ApplyFilter.SESSION), context);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
