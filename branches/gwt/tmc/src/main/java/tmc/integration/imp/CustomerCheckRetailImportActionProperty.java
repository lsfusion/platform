package tmc.integration.imp;

import org.xBaseJ.DBF;
import platform.interop.action.ClientAction;
import platform.server.auth.PolicyManager;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ClassFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
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

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {

        DBF impFile = null;

        try {

            impFile = new DBF("impcustomer.dbf");
            int recordCount = impFile.getRecordCount();

            FormInstance formInstance = new FormInstance(
                    new ClassFormEntity(BL.LM, BL.VEDLM.customerCheckRetail),
                    BL, BL.createSession(), PolicyManager.serverSecurityPolicy, null, null,
                    new DataObject(executeForm.form.instanceFactory.computer, BL.LM.computer));

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                formInstance.addObject(BL.VEDLM.customerCheckRetail);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.LM.barcode), new String(impFile.getField("barcode").getBytes(), "Cp1251"), false);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.LM.name), new String(impFile.getField("name").getBytes(), "Cp1251"), false);
                formInstance.changeProperty(formInstance.getPropertyDraw(BL.VEDLM.clientInitialSum), Double.parseDouble(impFile.getField("clientsum").get()), false);
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
