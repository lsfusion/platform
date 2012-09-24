package platform.server.logics.property.actions.flow;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ApplyActionProperty extends ChangeFlowActionProperty {
    private final BusinessLogics BL;
    private final CalcProperty canceled;

    public ApplyActionProperty(BusinessLogics BL, CalcProperty canceled) {
        super("apply", "apply");
        
        this.BL = BL;
        this.canceled = canceled;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.APPLY;
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        if(!context.apply(BL) && canceled!=null) // если apply'ся то canceled по опререлению сбросится
            canceled.change(context, true);
        return FlowResult.FINISH;
    }
}

