package lsfusion.server.logics.form.interactive.action.seek;

import lsfusion.base.col.MapFact;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class SeekObjectAction extends SeekAction {
    
    private final ObjectEntity object;
    private final UpdateType type;

    public SeekObjectAction(ObjectEntity object, UpdateType type) {
        super(LocalizedString.NONAME, object.baseClass);

        this.object = object;
        this.type = type;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.seekObjects(null, MapFact.singleton(form.instanceFactory.getInstance(this.object), context.getSingleKeyValue()), type);
    }
}
