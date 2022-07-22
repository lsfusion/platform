package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.interop.action.CloseFormClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class CloseFormAction extends SystemExplicitAction {

    private String formId;

    public CloseFormAction(LocalizedString caption, String formId) {
        super(caption);
        this.formId = formId;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new CloseFormClientAction(formId));
    }
}