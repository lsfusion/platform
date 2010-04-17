package platform.client.form;

import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.ClientGroupObjectImplementView;
import platform.client.logics.ClientCellView;

import java.util.List;

public interface LogicsSupplier {

    List<ClientObjectImplementView> getObjects();
    ClientGroupObjectImplementView getGroupObject();
    List<ClientPropertyView> getGroupObjectProperties();
    List<ClientPropertyView> getProperties();
    List<ClientCellView> getCells();

    // пока зафигачим в этот интерфейс, хотя может быть в дальнейшем выделим в отдельный
    // данный интерфейс отвечает за получение текущих выбранных значений
    ClientPropertyView getDefaultProperty();
    Object getSelectedValue(ClientPropertyView property);
    
    ClientForm getForm();
}
