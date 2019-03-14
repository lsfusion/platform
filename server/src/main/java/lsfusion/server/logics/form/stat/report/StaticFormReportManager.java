package lsfusion.server.logics.form.stat.report;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class StaticFormReportManager extends FormReportManager {
    
    public StaticFormReportManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(new StaticFormReportInterface(form, mapObjects, context));
    }
}
