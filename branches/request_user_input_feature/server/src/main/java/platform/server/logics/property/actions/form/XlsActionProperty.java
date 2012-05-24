package platform.server.logics.property.actions.form;

import platform.interop.action.RunExcelClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;

import java.sql.SQLException;

public class XlsActionProperty extends CustomActionProperty {

    public XlsActionProperty() {
//        super("formXlsAction", ApiResourceBundle.getString("form.layout.xls"), new ValueClass[0]);
        super("formXlsAction", "", new ValueClass[0]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.addAction(new RunExcelClientAction());
    }
}
