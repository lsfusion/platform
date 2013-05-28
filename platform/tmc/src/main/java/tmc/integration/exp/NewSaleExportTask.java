package tmc.integration.exp;

import platform.base.col.MapFact;
import platform.interop.form.ServerResponse;
import platform.server.classes.LogicalClass;
import platform.server.form.instance.CalcPropertyObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.filter.NotFilterInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class NewSaleExportTask extends AbstractSaleExportTask {

    public NewSaleExportTask(ExecutionContext<ClassPropertyInterface> context, String path, Integer store) {
        super(context, path, store);
    }

    protected String getDbfName() {
        return "datacur.dbf";
    }

    protected void setRemoteFormFilter(FormInstance formInstance) {
        PropertyDrawInstance<?> exported = formInstance.getPropertyDraw(BL.VEDLM.checkRetailExported);
        exported.toDraw.addTempFilter(new NotFilterInstance(new NotNullFilterInstance((CalcPropertyObjectInstance) exported.propertyObject)));
    }

    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
        formInstance.executeEditAction(formInstance.getPropertyDraw(BL.VEDLM.checkRetailExported), ServerResponse.GROUP_CHANGE,
                MapFact.<ObjectInstance, DataObject>EMPTY(), new DataObject(true, LogicalClass.instance), null, true);
    }
}
