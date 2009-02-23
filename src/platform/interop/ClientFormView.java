package platform.interop;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ClientFormView implements Serializable {

    public List<ClientGroupObjectImplement> groupObjects = new ArrayList<ClientGroupObjectImplement>();
    public List<ClientObjectImplement> objects = new ArrayList<ClientObjectImplement>();
    public List<ClientPropertyView> properties = new ArrayList<ClientPropertyView>();

    public List<ClientContainerView> containers = new ArrayList<ClientContainerView>();

    public LinkedHashMap<ClientPropertyView,Boolean> defaultOrders = new LinkedHashMap<ClientPropertyView, Boolean>();
    public List<ClientRegularFilterGroupView> regularFilters = new ArrayList<ClientRegularFilterGroupView>();

    public ClientFunctionView printView = new ClientFunctionView();
    public ClientFunctionView refreshView = new ClientFunctionView();
    public ClientFunctionView applyView = new ClientFunctionView();
    public ClientFunctionView cancelView = new ClientFunctionView();
    public ClientFunctionView okView = new ClientFunctionView();
    public ClientFunctionView closeView = new ClientFunctionView();

    public List<ClientCellView> order = new ArrayList<ClientCellView>();

    public ClientFormView() {
    }

    public ClientGroupObjectImplement getGroupObject(int id) {
        for (ClientGroupObjectImplement groupObject : groupObjects)
            if (groupObject.ID == id) return groupObject;
        return null;
    }

    public ClientPropertyView getPropertyView(int id) {
        for (ClientPropertyView property : properties)
            if (property.ID == id) return property;
        return null;
    }
}
