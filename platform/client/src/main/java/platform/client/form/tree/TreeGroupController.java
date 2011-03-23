package platform.client.form.tree;

import platform.base.BaseUtils;
import platform.client.form.*;
import platform.client.form.cell.PropertyController;
import platform.client.form.panel.PanelController;
import platform.client.form.panel.PanelToolbar;
import platform.client.form.queries.FilterController;
import platform.client.logics.*;
import platform.interop.ClassViewType;
import platform.interop.Order;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TreeGroupController implements GroupObjectLogicsSupplier {
    private PanelController panel;

    public final ClientTreeGroup treeGroup;
    private final LogicsSupplier logicsSupplier;
    private final ClientFormController form;
    private final TreeView view;

    private PanelToolbar panelToolbar;

    private TreeGroupTable tree;

    private final ClientGroupObject lastGroupObject;

    public TreeGroupController(ClientTreeGroup itreeGroup, LogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout formLayout) throws IOException {
        treeGroup = itreeGroup;
        logicsSupplier = ilogicsSupplier;
        form = iform;

        view = new TreeView(this.form, treeGroup);
        tree = view.getTree();

        panelToolbar = new PanelToolbar(form, formLayout);

        panel = new PanelController(this, form, formLayout);

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(view, BorderLayout.CENTER);
        pane.add(panelToolbar.getView(), BorderLayout.SOUTH);

        lastGroupObject = BaseUtils.last(treeGroup.groups);

        if (!treeGroup.plainTreeMode) {
            FilterController filterController = new FilterController(this) {

                protected boolean queryChanged() {

                    try {
                        form.changeFilter(treeGroup, getConditions());
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при применении фильтра", e);
                    }

                    tree.requestFocusInWindow();
                    return true;
                }
            };
            addToToolbar(filterController.getView());
            filterController.getView().addActions(tree);
        }

        formLayout.add(treeGroup, pane);
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects) {

        tree.saveVisualState();
        for (ClientGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                view.updateKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            // добавляем новые свойства
            for (ClientPropertyReader read : fc.properties.keySet()) {
                if (read instanceof ClientPropertyDraw) {
                    ClientPropertyDraw property = (ClientPropertyDraw) read;
                    if (property.groupObject == group && property.shouldBeDrawn(form)) {
                        addDrawProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panelProperties.contains(property)) {
                            panel.updateColumnKeys(property, Collections.singletonList(new ClientGroupObjectValue()));
                        }
                    }
                }
            }

            // удаляем ненужные
            for (ClientPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            // обновляем значения свойств
            for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                ClientPropertyReader propertyRead = readProperty.getKey();
                if (propertyRead instanceof ClientPropertyDraw) {
                    ClientPropertyDraw propertyDraw = (ClientPropertyDraw) propertyRead;
                    if (propertyDraw.groupObject == group) {
                        if (panelProperties.contains(propertyDraw)) {
                            panel.updatePropertyValues(propertyDraw, readProperty.getValue());
                        } else {
                            view.updateDrawPropertyValues(propertyDraw, readProperty.getValue());
                        }
                    }
                }
            }

            if (fc.objects.containsKey(group)) {
                view.setCurrentObjects(fc.objects.get(group));
            }
        }

        panel.update();
        tree.restoreVisualState();
    }

    private Set<ClientPropertyDraw> panelProperties = new HashSet<ClientPropertyDraw>();

    public void addDrawProperty(ClientGroupObject group, ClientPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            panelProperties.add(property);
            panel.addProperty(property);
            view.removeProperty(group, property);
        } else {
            panelProperties.remove(property);
            view.addDrawProperty(group, property, toPanel);
            panel.removeProperty(property);
        }
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        panelProperties.remove(property);
        view.removeProperty(group, property);
        panel.removeProperty(property);
    }

    @Override
    public ClientGroupObject getGroupObject() {
        return lastGroupObject;
    }

    @Override
    public List<ClientPropertyDraw> getGroupObjectProperties() {
        ClientGroupObject currentGroupObject = getSelectedGroupObject();

        ArrayList<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (currentGroupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    @Override
    public ClientPropertyDraw getSelectedProperty() {
        ClientPropertyDraw defaultProperty = lastGroupObject.filterProperty;
        return defaultProperty != null
                ? defaultProperty
                : tree.getCurrentProperty();
    }

    @Override
    public Object getSelectedValue(ClientPropertyDraw property) {
        return tree.getSelectedValue(property);
    }

    @Override
    public ClientFormController getForm() {
        return form;
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        tree.changeOrder(property, modiType);
    }

    @Override
    public List<ClientObject> getObjects() {
        return logicsSupplier.getObjects();
    }

    @Override
    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    @Override
    public ClientGroupObject getSelectedGroupObject() {
        Object node = tree.currentTreePath.getLastPathComponent();
        return node instanceof TreeGroupNode
               ? ((TreeGroupNode) node).group
               : treeGroup.groups.get(0);
    }

    @Override
    public void updateToolbar() {
        panelToolbar.update(ClassViewType.GRID);
    }

    @Override
    public void addPropertyToToolbar(PropertyController property) {
        panelToolbar.addProperty(property);
    }

    @Override
    public void removePropertyFromToolbar(PropertyController property) {
        panelToolbar.removeProperty(property);
    }

    public void addToToolbar(JComponent component) {
        panelToolbar.addComponent(component);
    }
}