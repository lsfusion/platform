package platform.server.view.form.client;

import platform.base.OrderedMap;
import platform.base.IDGenerator;
import platform.base.DefaultIDGenerator;
import platform.server.view.navigator.CellViewNavigator;
import platform.server.view.navigator.PropertyViewNavigator;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FormView implements ClientSerialize {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator = new DefaultIDGenerator();

    public Collection<ContainerView> containers = new ArrayList<ContainerView>();

    protected ContainerView addContainer() {
        ContainerView container = new ContainerView(idGenerator.idShift());
        containers.add(container);
        return container;
    }

    // список групп
    public List<GroupObjectImplementView> groupObjects = new ArrayList<GroupObjectImplementView>();

    // список свойств
    public List<PropertyCellView> properties = new ArrayList<PropertyCellView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    public OrderedMap<CellViewNavigator,Boolean> defaultOrders = new OrderedMap<CellViewNavigator, Boolean>();

    public FunctionView printView = new FunctionView(idGenerator.idShift());
    public FunctionView xlsView = new FunctionView(idGenerator.idShift());
    public FunctionView refreshView = new FunctionView(idGenerator.idShift());
    public FunctionView applyView = new FunctionView(idGenerator.idShift());
    public FunctionView cancelView = new FunctionView(idGenerator.idShift());
    public FunctionView okView = new FunctionView(idGenerator.idShift());
    public FunctionView closeView = new FunctionView(idGenerator.idShift());

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
        serializeList(outStream, properties);
        serializeList(outStream,regularFilters);

        outStream.writeInt(defaultOrders.size());
        for(Map.Entry<CellViewNavigator,Boolean> order : defaultOrders.entrySet()) {
            outStream.writeBoolean(order.getKey() instanceof PropertyViewNavigator);
            outStream.writeInt(order.getKey().ID);
            outStream.writeBoolean(order.getValue());
        }

        printView.serialize(outStream);
        xlsView.serialize(outStream);
        refreshView.serialize(outStream);
        applyView.serialize(outStream);
        cancelView.serialize(outStream);
        okView.serialize(outStream);
        closeView.serialize(outStream);

        outStream.writeInt(order.size());
        for(CellView orderCell : order) {
            outStream.writeInt(orderCell.getID());
            if (orderCell instanceof PropertyCellView)
                outStream.writeBoolean(true);
            else {
                outStream.writeBoolean(false);
                outStream.writeBoolean(orderCell instanceof ClassCellView);
            }
        }
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {

        if (comp1.container != comp2.container)
            throw new RuntimeException("Запрещено создавать пересечения для объектов в разных контейнерах");
        comp1.constraints.intersects.put(comp2, cons);
    }
}
