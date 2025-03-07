package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class OkAction extends FormFlowAction {
    private static LP showIf = createIfProperty(new Property[]{FormEntity.showOk, FormEntity.isEditing}, new boolean[]{false, true});

    public OkAction(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formOk(context);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.PRIMARY)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }

    @Override
    protected String getValueElementClass() {
        return "btn-primary";
    }
}
