package tmc.integration.exp;

import tmc.VEDBusinessLogics;
import platform.server.view.form.RemoteForm;
import platform.server.view.form.PropertyView;
import platform.server.view.form.filter.NotNullFilter;
import platform.server.view.form.filter.NotFilter;

import java.sql.SQLException;

public class NewSaleExportTask extends AbstractSaleExportTask {

    protected NewSaleExportTask(VEDBusinessLogics BL, String path) {
        super(BL, path);
    }

    protected String getDbfName() {
        return "datacur.dbf";
    }

    protected void setRemoteFormFilter(RemoteForm remoteForm) {
        PropertyView<?> exported = remoteForm.getPropertyView(BL.checkRetailExported);
        exported.toDraw.addTempFilter(new NotFilter(new NotNullFilter(exported.view)));
    }

    protected void updateRemoteFormProperties(RemoteForm remoteForm) throws SQLException {
        remoteForm.changeProperty(remoteForm.getPropertyView(BL.checkRetailExported), true, null, true);
    }
}
