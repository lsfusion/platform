package lsfusion.server.logics.property.actions.form;

import lsfusion.interop.action.RunOpenInExcelClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RunReportXlsActionProperty extends ReportClientActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDialog}, new boolean[] {false, true});

    public RunReportXlsActionProperty(BaseLogicsModule lm) {
        super(lm, false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInterfaction(new RunOpenInExcelClientAction());
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
