package lsfusion.client.form.property;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;

import java.util.Map;

public interface ClientPropertyReader {
    void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller);

    ClientGroupObject getGroupObject();

    // for converting to gwt
    int getID();
    byte getType();
}
