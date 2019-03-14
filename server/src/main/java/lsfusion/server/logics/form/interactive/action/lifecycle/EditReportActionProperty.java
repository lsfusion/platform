package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.interop.action.RunEditReportClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class EditReportActionProperty extends ReportClientActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isDebug, FormEntity.isFloat}, new boolean[] {false, true});

    public EditReportActionProperty(BaseLogicsModule lm) {
        super(lm, false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInterfaction(new RunEditReportClientAction(context.getFormInstance(true, true).getCustomReportPathList()));
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
