package platform.client.form;

import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientCellView;

import java.util.List;

public interface LogicsSupplier {

    List<ClientObjectImplementView> getObjects();
    List<ClientPropertyView> getProperties();
    List<ClientCellView> getCells();
}
