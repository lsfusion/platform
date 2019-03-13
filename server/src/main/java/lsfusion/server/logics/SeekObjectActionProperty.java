package lsfusion.server.logics;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class SeekObjectActionProperty extends SeekActionProperty {
    
    private final ObjectEntity object;
    private final UpdateType type;

    public SeekObjectActionProperty(ObjectEntity object, UpdateType type) {
        super(LocalizedString.concatList("Найти объект (", object.getCaption(), ")"), object.baseClass);

        this.object = object;
        this.type = type;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue value = context.getSingleKeyValue();

        ObjectInstance object = form.instanceFactory.getInstance(this.object);
        object.groupTo.seek(type);
        form.seekObject(object, value, type);
    }
}
