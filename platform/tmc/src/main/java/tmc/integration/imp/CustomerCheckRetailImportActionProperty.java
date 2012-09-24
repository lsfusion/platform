package tmc.integration.imp;

import org.xBaseJ.DBF;
import platform.server.auth.PolicyManager;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ListFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import tmc.VEDBusinessLogics;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerCheckRetailImportActionProperty extends UserActionProperty {

    VEDBusinessLogics BL;

    public CustomerCheckRetailImportActionProperty(VEDBusinessLogics BL, String sID) {
        super(sID, "Импорт покупателей", new ValueClass[] {});
        this.BL = BL;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.emitExceptionIfNotInFormSession();

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

                DataObject dataObject = context.addObject(BL.VEDLM.customerCheckRetail);
                BL.LM.barcode.change(new String(impFile.getField("barcode").getBytes(), "Cp1251"), context, dataObject);
                BL.LM.name.change(new String(impFile.getField("name").getBytes(), "Cp1251"), context, dataObject);
                BL.VEDLM.clientInitialSum.change(Double.parseDouble(impFile.getField("clientsum").get()), context, dataObject);
            }

            context.apply(BL);

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
