package platform.client.form;

import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;

import java.util.List;

public interface GroupObjectLogicsSupplier extends LogicsSupplier {

    ClientGroupObject getGroupObject();
    List<ClientPropertyDraw> getGroupObjectProperties();

    // пока зафигачим в этот интерфейс, хотя может быть в дальнейшем выделим в отдельный
    // данный интерфейс отвечает за получение текущих выбранных значений
    ClientPropertyDraw getSelectedProperty();
    Object getSelectedValue(ClientPropertyDraw property);
    
    ClientFormController getForm();

    ClientGroupObject getSelectedGroupObject();

    void updateToolbar();

    void addPropertyToToolbar(PropertyController property);

    void removePropertyFromToolbar(PropertyController property);
}
