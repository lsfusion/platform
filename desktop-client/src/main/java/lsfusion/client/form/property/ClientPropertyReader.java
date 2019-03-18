package lsfusion.client.form.property;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.TableController;

import java.util.Map;

public interface ClientPropertyReader {
    void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller);

    ClientGroupObject getGroupObject();

    boolean shouldBeDrawn(ClientFormController form);

    int getID();
    byte getType();
}
