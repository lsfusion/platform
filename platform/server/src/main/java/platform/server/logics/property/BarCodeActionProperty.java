package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.RemoteForm;
import platform.interop.action.ClientAction;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;

public class BarCodeActionProperty extends ActionProperty {

    final Property<?> barcodeToObject;

    public BarCodeActionProperty(String sID, String caption, Property<?> barcodeToObject) {
        super(sID, caption, new ValueClass[]{StringClass.get(13)});

        this.barcodeToObject = barcodeToObject;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, List<ClientAction> actions, RemoteFormView executeForm) throws SQLException {
        ((RemoteForm<?>)executeForm.form).executeBarcode(BaseUtils.singleValue(keys), barcodeToObject);
    }
}
