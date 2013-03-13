package tmc.integration.imp;

import org.xBaseJ.DBF;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ListFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.SecurityManager;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.UserActionProperty;
import tmc.VEDBusinessLogics;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerCheckRetailImportActionProperty extends UserActionProperty {

    public CustomerCheckRetailImportActionProperty(String sID) {
        super(sID, "Импорт покупателей", new ValueClass[] {});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        VEDBusinessLogics BL = (VEDBusinessLogics) context.getBL();

        context.emitExceptionIfNotInFormSession();

        DBF impFile = null;

        try {

            impFile = new DBF("impcustomer.dbf");
            int recordCount = impFile.getRecordCount();

            FormInstance formInstance = new FormInstance(
                    new ListFormEntity(BL.LM, BL.VEDLM.customerCheckRetail),
                    context.getLogicsInstance(), context.createSession(), SecurityManager.serverSecurityPolicy, null, null,
                    new DataObject(context.getFormInstance().instanceFactory.computer, BL.authenticationLM.computer), null);

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                DataObject dataObject = context.addObject(BL.VEDLM.customerCheckRetail);
                BL.VEDLM.barcode.change(new String(impFile.getField("barcode").getBytes(), "Cp1251"), context, dataObject);
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
