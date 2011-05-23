package tmc.integration.imp;

import org.xBaseJ.DBF;
import platform.interop.action.ClientAction;
import platform.server.auth.PolicyManager;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ClassFormEntity;
import platform.server.form.entity.DialogFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import tmc.VEDBusinessLogics;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CustomerCheckRetailImportActionProperty extends ActionProperty {

    VEDBusinessLogics BL;

    public CustomerCheckRetailImportActionProperty(VEDBusinessLogics BL, String sID) {
        super(sID, "Импорт покупателей", new ValueClass[] {});
        this.BL = BL;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

        DBF impFile = null;

        try {

            impFile = new DBF("impcustomer.dbf");
            int recordCount = impFile.getRecordCount();

            FormInstance formInstance = new FormInstance(
                    new ClassFormEntity(BL, BL.customerCheckRetail),
                    BL, BL.createSession(), PolicyManager.serverSecurityPolicy, null, null,
                    new DataObject(executeForm.form.instanceFactory.computer, BL.computer));

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                formInstance.addObject(BL.customerCheckRetail);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.barcode), new String(impFile.getField("barcode").getBytes(), "Cp1251"));
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.name), new String(impFile.getField("name").getBytes(), "Cp1251"));
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.clientInitialSum), Double.parseDouble(impFile.getField("clientsum").get()));
            }

            formInstance.applyActionChanges(actions);

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
