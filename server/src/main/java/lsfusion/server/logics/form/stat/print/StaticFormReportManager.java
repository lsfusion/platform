package lsfusion.server.logics.form.stat.print;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.sql.SQLException;

public class StaticFormReportManager extends FormReportManager {
    
    public StaticFormReportManager(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context, ImSet<ContextFilterInstance> contextFilters) {
        super(new StaticFormReportInterface(form, mapObjects, context, contextFilters));
    }

    public String readFormCaption() throws SQLException, SQLHandledException {
        FormEntity form = reportInterface.getFormEntity();

        FormView richDesign = form.getRichDesign();
        PropertyObjectEntity<?> propertyCaption = richDesign.mainContainer.propertyCaption;
        if(propertyCaption != null)
            return BaseUtils.nullToString(reportInterface.read(propertyCaption));

        return ThreadLocalContext.localize(richDesign.mainContainer.caption);
    }
}
