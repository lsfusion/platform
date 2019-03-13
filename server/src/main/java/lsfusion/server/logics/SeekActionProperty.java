package lsfusion.server.logics;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitActionProperty;

import java.sql.SQLException;

public abstract class SeekActionProperty extends SystemExplicitActionProperty {

    public SeekActionProperty(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
    
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(false, true);
        executeForm(formInstance, context);
    }

    protected abstract void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;
}
