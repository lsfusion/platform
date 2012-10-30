package tmc.integration.exp;

import platform.interop.form.ServerResponse;
import platform.server.classes.LogicalClass;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.NotFilterInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.logics.DataObject;
import tmc.VEDBusinessLogics;

import java.sql.SQLException;
import java.util.HashMap;

public class NewSaleExportTask extends AbstractSaleExportTask {

    public NewSaleExportTask(VEDBusinessLogics BL, String path, Integer store) {
        super(BL, path, store);
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
                new HashMap<ObjectInstance, DataObject>(), new DataObject(true, LogicalClass.instance), null, true);
    }
}
