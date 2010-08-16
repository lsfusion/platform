package platform.client.form;

import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;

import java.util.List;

public interface GroupObjectLogicsSupplier extends LogicsSupplier {

    ClientGroupObject getGroupObject();
    List<ClientPropertyDraw> getGroupObjectProperties();

    // пока зафигачим в этот интерфейс, хотя может быть в дальнейшем выделим в отдельный
    // данный интерфейс отвечает за получение текущих выбранных значений
    ClientPropertyDraw getDefaultProperty();
    Object getSelectedValue(ClientPropertyDraw property);
    
    ClientFormController getForm();
}
