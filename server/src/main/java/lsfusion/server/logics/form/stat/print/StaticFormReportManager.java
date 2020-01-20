package lsfusion.server.logics.form.stat.print;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class StaticFormReportManager extends FormReportManager {
    
    public StaticFormReportManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context, ImSet<ContextFilterInstance> contextFilters) {
        super(new StaticFormReportInterface(form, mapObjects, context, contextFilters));
    }
}
