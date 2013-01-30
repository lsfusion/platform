package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class ApplyActionProperty extends SystemActionProperty {
    private final BusinessLogics BL;
    private final CalcProperty canceled;

    public ApplyActionProperty(BusinessLogics BL, CalcProperty canceled) {
        super("apply", "apply", new ValueClass[]{});
        
        this.BL = BL;
        this.canceled = canceled;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.APPLY;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        if(!context.apply(BL) && canceled!=null) // если apply'ся то canceled по опререлению сбросится
            canceled.change(context, true);
    }
}

