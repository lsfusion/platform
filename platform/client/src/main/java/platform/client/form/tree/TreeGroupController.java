package platform.client.form.tree;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.PanelLogicsSupplier;
import platform.client.form.cell.PropertyController;
import platform.client.form.panel.PanelToolbar;
import platform.client.form.panel.PanelController;
import platform.client.logics.*;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TreeGroupController implements PanelLogicsSupplier {
    private PanelController panel;

    public final ClientTreeGroup treeGroup;
    private final ClientFormController form;
    private final TreeView view;

    private PanelToolbar panelToolbar;

    public TreeGroupController(ClientTreeGroup treeGroup, ClientFormController form, ClientFormLayout formLayout) throws IOException {
        this.treeGroup = treeGroup;
        this.form = form;

        view = new TreeView(form, treeGroup);

        panelToolbar = new PanelToolbar(form, formLayout);

        panel = new PanelController(this, form, formLayout);

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(view, BorderLayout.CENTER);
        pane.add(panelToolbar.getView(), BorderLayout.SOUTH);

        formLayout.add(treeGroup, pane);
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects) {

        view.getTree().saveVisualState();
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
        view.getTree().restoreVisualState();
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
    public void updateToolbar() {
        panelToolbar.update(ClassViewType.GRID);
    }

    @Override
    public ClientGroupObject getGroupObject() {
        Object node = view.getTree().currentTreePath.getLastPathComponent();
        return node instanceof TreeGroupNode
                ? ((TreeGroupNode) node).group
                : null;
    }

    @Override
    public void addPropertyToToolbar(PropertyController property) {
        panelToolbar.addProperty(property);
    }

    @Override
    public void removePropertyFromToolbar(PropertyController property) {
        panelToolbar.removeProperty(property);
    }
}