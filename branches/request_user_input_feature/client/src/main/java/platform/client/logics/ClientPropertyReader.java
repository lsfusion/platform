package platform.client.logics;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.interop.ClassViewType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ClientPropertyReader {

    List<ClientObject> getKeysObjectsList(Set<ClientPropertyReader> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers);

    void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectLogicsSupplier controller);

    ClientGroupObject getGroupObject();

    boolean shouldBeDrawn(ClientFormController form);    
}
