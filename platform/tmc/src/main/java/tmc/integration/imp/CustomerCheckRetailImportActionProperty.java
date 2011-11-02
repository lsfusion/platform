package tmc.integration.imp;

import org.xBaseJ.DBF;
import platform.server.auth.PolicyManager;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ListFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ExecutionContext;
import tmc.VEDBusinessLogics;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerCheckRetailImportActionProperty extends ActionProperty {

    VEDBusinessLogics BL;

    public CustomerCheckRetailImportActionProperty(VEDBusinessLogics BL, String sID) {
        super(sID, "Импорт покупателей", new ValueClass[] {});
        this.BL = BL;
    }

    public void execute(ExecutionContext context) throws SQLException {

        DBF impFile = null;

        try {

            impFile = new DBF("impcustomer.dbf");
            int recordCount = impFile.getRecordCount();

            FormInstance formInstance = new FormInstance(
                    new ListFormEntity(BL.LM, BL.VEDLM.customerCheckRetail),
                    BL, BL.createSession(), PolicyManager.serverSecurityPolicy, null, null,
                    new DataObject(context.getFormInstance().instanceFactory.computer, BL.LM.computer));

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                formInstance.addObject(BL.VEDLM.customerCheckRetail);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.LM.barcode), new String(impFile.getField("barcode").getBytes(), "Cp1251"), false);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.LM.name), new String(impFile.getField("name").getBytes(), "Cp1251"), false);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.VEDLM.clientInitialSum), Double.parseDouble(impFile.getField("clientsum").get()), false);
            }

            formInstance.synchronizedApplyChanges(null, context.getActions());

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (impFile != null)
                try {
                    impFile.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }

    }
}
