package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import platform.gwt.form2.shared.view.GForm;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GTreeGroup;
import platform.gwt.form2.shared.view.changes.GFormChanges;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form2.shared.view.reader.*;

import java.util.Map;

public class GTreeGroupController implements GGroupObjectLogicsSupplier {
    private GFormController formController;
    private GTreeGroup treeGroup;
    private Panel treeTableView;

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

        treeTableView = new VerticalPanel();

        treeTableView.add(tree);
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
                    if (property.groupObject == group) {
                        if (fc.panelProperties.contains(property)) {
                            addPanelProperty(group, property);
                        } else {
                            addGridProperty(group, property);
                        }
                    }
                }
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            for (GPropertyReader propertyReader : fc.properties.keySet()) {
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == group) {
                    propertyReader.update(this, fc.properties.get(propertyReader));
                }
            }
        }
        update();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        panel.removeProperty(property);
        tree.removeProperty(group, property);
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

    @Override
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.setValue(property, values);
        } else {
            tree.setValues(property, values);
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
}
