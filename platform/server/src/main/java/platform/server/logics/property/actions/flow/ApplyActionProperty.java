package platform.server.logics.property.actions.flow;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

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

