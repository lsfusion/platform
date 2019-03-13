package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.action.ExecutionContext;

public class StaticFormDataManager extends FormDataManager {

    public StaticFormDataManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(new StaticFormDataInterface(form, mapObjects, context));
    }
}
