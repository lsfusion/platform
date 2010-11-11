package platform.client.logics;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.interop.ClassViewType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ClientPropertyRead {

    List<ClientObject> getDeserializeList(Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers);

    void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller);

    ClientGroupObject getGroupObject();

    boolean shouldBeDrawn(ClientFormController form);    
}
