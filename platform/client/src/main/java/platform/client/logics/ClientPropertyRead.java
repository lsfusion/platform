package platform.client.logics;

import platform.client.form.GroupObjectController;
import platform.client.form.ClientFormController;

import java.util.List;
import java.util.Set;
import java.util.Map;

public interface ClientPropertyRead {

    List<ClientObject> getDeserializeList(Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, Byte> classViews, Map<ClientGroupObject, GroupObjectController> controllers);

    void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller);

    ClientGroupObject getGroupObject();

    boolean shouldBeDrawn(ClientFormController form);    
}
