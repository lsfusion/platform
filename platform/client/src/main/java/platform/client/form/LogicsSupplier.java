package platform.client.form;

import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientObjectImplementView;

import java.util.List;

public interface LogicsSupplier {

    List<ClientObjectImplementView> getObjects();
    List<ClientPropertyView> getGroupObjectProperties();
    List<ClientPropertyView> getProperties();
    ClientPropertyView getDefaultProperty();

    // пока зафигачим в этот интерфейс, хотя может быть в дальнейшем выделим в отдельный
    ClientForm getForm();
    Object getSelectedValue(ClientPropertyView property);
}
