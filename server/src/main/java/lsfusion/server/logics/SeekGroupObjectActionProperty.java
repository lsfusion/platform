package lsfusion.server.logics;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class SeekGroupObjectActionProperty extends ScriptingActionProperty {

    private final GroupObjectEntity groupObject;
    private boolean last;

    @SuppressWarnings("UnusedDeclaration")
    public SeekGroupObjectActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);

        groupObject = null;
    }

    public SeekGroupObjectActionProperty(ScriptingLogicsModule lm, GroupObjectEntity groupObject, boolean last) {
        super(lm, "Найти объект (" + groupObject.getSID() + ")");

        this.groupObject = groupObject;
        this.last = last;
    }

    @Override
    protected boolean allowNulls() {
        return groupObject != null;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.emitExceptionIfNotInFormSession();

        if (groupObject != null)
            groupObject.getInstance(context.getFormInstance().instanceFactory).addSeek(last);
    }
}