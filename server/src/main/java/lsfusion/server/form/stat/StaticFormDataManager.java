package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;

public class StaticFormDataManager extends FormDataManager {

    public StaticFormDataManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(new StaticFormDataInterface(form, mapObjects, context));
    }
}
