package tmc.integration.exp;

import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import tmc.VEDBusinessLogics;
import platform.server.form.instance.filter.NotFilterInstance;

import java.sql.SQLException;

public class NewSaleExportTask extends AbstractSaleExportTask {

    protected NewSaleExportTask(VEDBusinessLogics BL, String path) {
        super(BL, path);
    }

    protected String getDbfName() {
        return "datacur.dbf";
    }

    protected void setRemoteFormFilter(FormInstance formInstance) {
        PropertyDrawInstance<?> exported = formInstance.getPropertyDraw(BL.checkRetailExported);
        exported.toDraw.addTempFilter(new NotFilterInstance(new NotNullFilterInstance(exported.propertyObject)));
    }

    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
        formInstance.changeProperty(formInstance.getPropertyDraw(BL.checkRetailExported), true, null, true);
    }
}
