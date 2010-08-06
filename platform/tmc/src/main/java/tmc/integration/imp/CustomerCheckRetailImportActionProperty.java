package tmc.integration.imp;

import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.RemoteForm;
import platform.server.view.navigator.ClassNavigatorForm;
import platform.server.classes.ValueClass;
import platform.server.auth.PolicyManager;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;
import java.io.IOException;

import org.xBaseJ.DBF;
import tmc.VEDBusinessLogics;

public class CustomerCheckRetailImportActionProperty extends ActionProperty {

    VEDBusinessLogics BL;

    public CustomerCheckRetailImportActionProperty(VEDBusinessLogics BL, String sID) {
        super(sID, "Импорт покупателей", new ValueClass[] {});
        this.BL = BL;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException {

        DBF impFile = null;

        try {

            impFile = new DBF("impcustomer.dbf");
            int recordCount = impFile.getRecordCount();

            RemoteForm remoteForm = new RemoteForm(
                    new ClassNavigatorForm(BL, BL.customerCheckRetail),
                    BL, BL.createSession(), PolicyManager.defaultSecurityPolicy, null, null,
                    new DataObject(executeForm.form.mapper.computer, BL.computer));

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                remoteForm.addObject(BL.customerCheckRetail);
                remoteForm.changeProperty(remoteForm.getPropertyView(BL.barcode), new String(impFile.getField("barcode").getBytes(), "Cp1251"));
                remoteForm.changeProperty(remoteForm.getPropertyView(BL.name), new String(impFile.getField("name").getBytes(), "Cp1251"));
                remoteForm.changeProperty(remoteForm.getPropertyView(BL.clientInitialSum), Double.parseDouble(impFile.getField("clientsum").get()));
            }

            String result = remoteForm.applyChanges();
            if (result != null)
                actions.add(new MessageClientAction(result, "Не удалось импортировать данные"));

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
