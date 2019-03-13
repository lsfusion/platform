package lsfusion.server.logics.form.stat.report;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.action.ExecutionContext;

public class StaticFormReportManager extends FormReportManager {
    
    public StaticFormReportManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(new StaticFormReportInterface(form, mapObjects, context));
    }
}
