package platform.client.logics;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;

import java.util.Map;

public interface ClientPropertyReader {
    void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectLogicsSupplier controller);

    ClientGroupObject getGroupObject();

    boolean shouldBeDrawn(ClientFormController form);    
}
