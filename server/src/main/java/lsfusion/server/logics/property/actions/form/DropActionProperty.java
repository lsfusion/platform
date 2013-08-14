package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class DropActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.showDrop}, new boolean[] {false});

    public DropActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formDrop();
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
