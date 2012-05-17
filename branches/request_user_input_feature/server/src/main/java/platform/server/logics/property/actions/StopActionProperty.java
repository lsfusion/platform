package platform.server.logics.property.actions;

import platform.interop.action.MessageClientAction;
import platform.interop.action.DenyCloseFormClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class StopActionProperty extends CustomActionProperty {

    private String header;

    public StopActionProperty(String sID, String caption, String header) {
        super(sID, caption, new ValueClass[] {});

        this.header = header;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.addAction(new MessageClientAction(caption, header));
        context.addAction(new DenyCloseFormClientAction());
    }
}
