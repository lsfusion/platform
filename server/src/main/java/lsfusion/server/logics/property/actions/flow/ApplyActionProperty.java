package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class ApplyActionProperty extends ScriptingActionProperty {
    private final CalcProperty canceled;

    public ApplyActionProperty(BaseLogicsModule lm) {
        super(lm, new ValueClass[]{});
        
        this.canceled = lm.getLCPByOldName("canceled").property;
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

