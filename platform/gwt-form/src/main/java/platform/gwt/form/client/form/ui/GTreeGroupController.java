package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GFormChanges;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.reader.*;

import java.util.HashMap;
import java.util.Map;

public class GTreeGroupController implements GGroupObjectLogicsSupplier {
    private GFormController formController;
    private GTreeGroup treeGroup;

    private GTreeTable tree;
    private GToolbarPanel treeToolbar;

    public GPanelController panel;
    public GToolbarPanel panelToolbar;

    public GGroupObject lastGroupObject;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm, GFormLayout iFormLayout) {
        formController = iFormController;
        treeGroup = iTreeGroup;
        lastGroupObject = treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;

        tree = new GTreeTable(iFormController, iForm);
        treeToolbar = new GToolbarPanel();

        panel = new GPanelController(iFormController, iFormLayout);
        panelToolbar = new GToolbarPanel();

        CellPanel treeTableView = new VerticalPanel();
        treeTableView.setSize("100%", "100%");

        ResizeLayoutPanel panel = new ResizeLayoutPanel();
        panel.setStyleName("gridResizePanel");
        panel.setSize("100%", "100%");
        panel.setWidget(tree);

        treeTableView.add(panel);
        treeTableView.setCellHeight(panel, "100%");
        treeTableView.setCellWidth(panel, "100%");
        treeTableView.add(treeToolbar);
        treeTableView.add(panelToolbar);

        iFormLayout.add(treeGroup, treeTableView);
    }

    public void processFormChanges(GFormChanges fc) {
        for (GGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            for (GPropertyReader propertyReader : fc.properties.keySet()) {
                if (propertyReader instanceof GPropertyDraw) {
                    GPropertyDraw property = (GPropertyDraw) propertyReader;
                    if (property.groupObject == group && !fc.updateProperties.contains(property)) {
                        addProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panel.containsProperty(property)) {
                            panel.updateColumnKeys(property, GGroupObjectValue.SINGLE_EMPTY_KEY_LIST);
                        }
                    }
                }
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                GPropertyReader propertyReader = readProperty.getKey();
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == group) {
                    propertyReader.update(this, readProperty.getValue(), fc.updateProperties.contains(propertyReader));
                }
            }
        }
        update();
    }

    public void rememberScrollPosition() {
        tree.rememberScrollPosition();
    }

    public void preparePendingState() {
        tree.preparePendingState();
    }

    public void applyPendingState() {
        tree.applyPendingState();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        panel.removeProperty(property);
        tree.removeProperty(group, property);
    }

    private void addProperty(GGroupObject group, GPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            addPanelProperty(group, property);
        } else {
            addGridProperty(group, property);
        }
    }

    private void addPanelProperty(GGroupObject group, GPropertyDraw property) {
        if (tree != null)
            tree.removeProperty(group, property);
        panel.addProperty(property);
    }

    private void addGridProperty(GGroupObject group, GPropertyDraw property) {
        if (tree != null)
            tree.addProperty(group, property);
        panel.removeProperty(property);
    }

    private void update() {
        if (tree != null) {
            tree.update();
        }
        panel.update();

        treeToolbar.setVisible(!treeToolbar.isEmpty());
        panelToolbar.setVisible(!panelToolbar.isEmpty());
    }

    public void redrawGridHeader() {
        tree.onResize();
    }

    @Override
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, updateKeys);
        } else {
            tree.updatePropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, values);
        } else {
            tree.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, values);
        } else {
            tree.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, values);
        } else {
            tree.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowBackgroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowBackgroundValue(values.values().iterator().next());
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowForegroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowForegroundValue(values.values().iterator().next());
    }

    @Override
    public boolean hasPanelProperty(GPropertyDraw property) {
        return panel.containsProperty(property);
    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return tree.getCurrentKey();
    }

    @Override
    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        tree.changeOrder(property, modiType);
    }
}
