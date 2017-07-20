package lsfusion.server.logics;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteValueClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class SeekObjectActionProperty extends SeekActionProperty {
    
    private final ObjectEntity object;
    private boolean last = false;

    public SeekObjectActionProperty(ObjectEntity object, boolean last) {
        super(LocalizedString.concatList("Найти объект (", object.getCaption(), ")"), object.baseClass);

        this.object = object;
        this.last = last;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue value = context.getSingleKeyValue();

        ObjectInstance object = form.instanceFactory.getInstance(this.object);
        object.groupTo.seek(last);
        form.seekObject(object, value, last);
    }
}
