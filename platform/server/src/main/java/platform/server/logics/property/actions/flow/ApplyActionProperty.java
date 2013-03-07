package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class ApplyActionProperty extends SystemActionProperty {
    private final CalcProperty canceled;

    public ApplyActionProperty(CalcProperty canceled) {
        super("apply", "apply", new ValueClass[]{});
        
        this.canceled = canceled;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.APPLY;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        if(!context.apply() && canceled!=null) // если apply'ся то canceled по опререлению сбросится
            canceled.change(context, true);
    }
}

