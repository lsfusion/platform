package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GTreeGroup;
import platform.gwt.view.changes.GFormChanges;

public class GTreeGroupController {
    private GTreeGroup treeGroup;
    private VLayout treeTableView;

    private GTreeTable tree;
    private GToolbarPanel treeToolbar;

    public GPanelController panel;
    public GToolbarPanel panelToolbar;

    public GGroupObject lastGroupObject;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm, GFormLayout iFormLayout) {
        treeGroup = iTreeGroup;
        lastGroupObject = treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;

        tree = new GTreeTable(iFormController, iForm);
        treeToolbar = new GToolbarPanel();

        panel = new GPanelController(iFormController, iForm, iFormLayout);
        panelToolbar = new GToolbarPanel();

        treeTableView = new VLayout();

        treeTableView.addMember(tree);
        treeTableView.addMember(treeToolbar);

        VLayout treePane = new VLayout();
        treePane.addMember(treeTableView);
        treePane.addMember(panelToolbar);

        iFormLayout.add(treeGroup, treePane);
    }

    public void processFormChanges(GFormChanges fc) {
        for (GGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            for (GPropertyDraw property : fc.properties.keySet()) {
                if (property.groupObject == group) {
                    if (fc.panelProperties.contains(property)) {
                        addPanelProperty(group, property);
                        panel.setValue(property, fc.properties.get(property));
                    } else {
                        addGridProperty(group, property);
                        tree.setValues(property, fc.properties.get(property));
                    }
                }
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
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
}
