package platform.server.logics.property.actions.form;

import platform.interop.action.PrintPreviewClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;

import java.sql.SQLException;

public class PrintActionProperty extends CustomActionProperty {

    public PrintActionProperty() {
//        super("formPrintAction", ApiResourceBundle.getString("form.layout.print"), new ValueClass[0]);
        super("formPrintAction", "", new ValueClass[0]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.addAction(new PrintPreviewClientAction());
    }
}
