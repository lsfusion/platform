package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.interop.action.CloseFormClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.action.FormAddress;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class CloseFormAction extends SystemExplicitAction {

    private final FormAddress address;

    public CloseFormAction(LocalizedString caption, FormAddress address) {
        super(caption);
        this.address = address;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new CloseFormClientAction(address.formId, address.formCanonicalName, address.windowCanonicalName));
    }
}
