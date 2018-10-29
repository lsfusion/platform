package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;

public class StaticFormReportManager extends FormReportManager {
    
    public StaticFormReportManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(new StaticFormReportInterface(form, mapObjects, context));
    }
}
