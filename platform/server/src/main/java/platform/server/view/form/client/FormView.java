package platform.server.view.form.client;

import platform.server.view.form.PropertyView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class FormView implements ClientSerialize {

    // именно в таком порядке сериализация
    // дерево container'ов
    public List<ContainerView> containers = new ArrayList<ContainerView>();
    // для id
    protected ContainerView addContainer() {
        ContainerView container = new ContainerView(containers.size());
        containers.add(container);
        return container;
    }

    // список групп
    public List<GroupObjectImplementView> groupObjects = new ArrayList<GroupObjectImplementView>();

    // список свойств
    public List<PropertyCellView> properties = new ArrayList<PropertyCellView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    public LinkedHashMap<PropertyView,Boolean> defaultOrders = new LinkedHashMap<PropertyView, Boolean>();

    public FunctionView printView = new FunctionView();
    public FunctionView refreshView = new FunctionView();
    public FunctionView applyView = new FunctionView();
    public FunctionView cancelView = new FunctionView();
    public FunctionView okView = new FunctionView();
    public FunctionView closeView = new FunctionView();

    public List<CellView> order = new ArrayList<CellView>();

    public FormView() {
    }

    static <T extends ClientSerialize> void serializeList(DataOutputStream outStream, Collection<T> list) throws IOException {
        outStream.writeInt(list.size());
        for(T element : list)
            element.serialize(outStream);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        List<ContainerView> orderedContainers = new ArrayList<ContainerView>();
        for(ContainerView container : containers)
            container.fillOrderList(orderedContainers);
        serializeList(outStream,orderedContainers);

        serializeList(outStream,groupObjects);
        serializeList(outStream,properties);
        serializeList(outStream,regularFilters);

        outStream.writeInt(defaultOrders.size());
        for(Map.Entry<PropertyView,Boolean> order : defaultOrders.entrySet()) {
            outStream.writeInt(order.getKey().ID);
            outStream.writeBoolean(order.getValue());
        }

        printView.serialize(outStream);
        refreshView.serialize(outStream);
        applyView.serialize(outStream);
        cancelView.serialize(outStream);
        okView.serialize(outStream);
        closeView.serialize(outStream);

        outStream.writeInt(order.size());
        for(CellView orderCell : order) {
            outStream.writeInt(orderCell.getID());
            outStream.writeBoolean(orderCell instanceof PropertyCellView);
        }
    }
}
