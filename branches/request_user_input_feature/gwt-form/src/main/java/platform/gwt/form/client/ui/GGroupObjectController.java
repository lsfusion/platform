package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GFormChanges;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.List;

public class GGroupObjectController {
    public GGroupObject groupObject;
    private GFormLayout formLayout;

    public GGridController grid;
    public GPanelController panel;
    public GToolbarPanel panelToolbar;
    private GToolbarPanel gridToolbar;
    private GClassViewType showType = GClassViewType.HIDE;
    private GShowTypeView showTypeView;

    public GGroupObjectController(GFormController iformController, GForm iform, GGroupObject igroupObject, GFormLayout formLayout) {
        groupObject = igroupObject;
        this.formLayout = formLayout;

        gridToolbar = new GToolbarPanel();

        panel = new GPanelController(iformController, iform, formLayout);

        panelToolbar = new GToolbarPanel();

        showTypeView = new GShowTypeView(igroupObject, iformController) {
            @Override
            public void hide() {
                GGroupObjectController.this.hide();
            }

            @Override
            public void show() {
                GGroupObjectController.this.show();
            }
        };

        if(groupObject != null) { 
            grid = new GGridController(groupObject.grid, iformController, iform, this, formLayout);
            grid.addView();

            formLayout.add(groupObject.grid.container, panelToolbar);
        }
        
        List<GClassViewType> banClassView = new ArrayList<GClassViewType>();
        if (groupObject != null) {
            for (String banView : groupObject.banClassView) {
                banClassView.add(GClassViewType.valueOf(banView));
            }
        }
        showTypeView.setBanClassView(banClassView);
    }

    public void processFormChanges(GFormChanges fc) {
        for (GPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        String classViewValue = fc.classViews.get(groupObject);
        if (classViewValue != null) {
            setClassView(GClassViewType.valueOf(classViewValue));
        }

        for (GPropertyDraw property : fc.properties.keySet()) {
            if (property.groupObject == groupObject) {
                if (fc.panelProperties.contains(property)) {
                    addPanelProperty(property);
                    panel.setValue(property, fc.properties.get(property));
                } else {
                    addGridProperty(property);
                    if (grid != null)
                        grid.getTable().setValues(property, fc.properties.get(property));
                }
            }
        }

        ArrayList<GGroupObjectValue> keys = fc.gridObjects.get(groupObject);
        if (keys != null && grid != null) {
            grid.getTable().setKeys(keys);
        }

        GGroupObjectValue currentKey = fc.objects.get(groupObject);
        if (currentKey != null && grid != null) {
            grid.getTable().setCurrentKey(currentKey);
        }

        update();
    }

    public void hide() {
        grid.hide();
        panel.hide();
        gridToolbar.removeComponent(showTypeView);
        showTypeView.addToToolbar(panelToolbar);
        for (Canvas component : panelToolbar.getComponents()) {
            if (!component.equals(showTypeView)) {
                component.setVisible(false);
            }
        }
    }

    public void show() {
        grid.show();
        panel.show();
        for (Canvas component : panelToolbar.getComponents()) {
            component.setVisible(true);
        }
    }

    public GShowTypeView getShowTypeView() {
        return showTypeView;
    }

    public void setClassView(GClassViewType classView) {
        if (classView != null && !classView.equals(showType)) {
            showType = classView;

            if (showTypeView != null && showTypeView.changeClassView(classView)) {
                updateToolbar();
                formLayout.resizeAll();
            }
        }
    }

    public GToolbarPanel getGridToolbar() {
        return gridToolbar;
    }

    private void removeProperty(GPropertyDraw property) {
        panel.removeProperty(property);
        if (grid != null)
            grid.getTable().removeProperty(property);
    }

    private void addGridProperty(GPropertyDraw property) {
        if (grid != null)
            grid.getTable().addProperty(property);
        panel.removeProperty(property);
    }

    private void addPanelProperty(GPropertyDraw property) {
        if (grid != null)
            grid.getTable().removeProperty(property);
        panel.addProperty(property);
    }

    public void addFilterComponent(FormItem item) {
        gridToolbar.addComponent(item);
    }

    public void addPanelFilterComponent(FormItem item) {
        panelToolbar.addComponent(item);
    }

    private void update() {
        if (grid != null) {
            grid.update();
        }
        panel.update();

        gridToolbar.setVisible(!gridToolbar.isEmpty());
        panelToolbar.setVisible(!panelToolbar.isEmpty());
    }

    public void updateToolbar() {
        if (groupObject != null) {
            if (isInGrid()) {
                panelToolbar.removeComponent(showTypeView);
                showTypeView.addToToolbar(gridToolbar);
                grid.show();
            } else {
                List<Canvas> list = new ArrayList<Canvas>(gridToolbar.getComponents());
                for (Canvas component : list) {
                    gridToolbar.removeComponent(component);
                    panelToolbar.addComponent(component);
                }
                showTypeView.addToToolbar(panelToolbar);
                grid.hide();
            }
        }
    }

    public boolean isInGrid() {
        return showType == GClassViewType.GRID;
    }

    public GGroupObjectValue getCurrentKey() {
        return grid != null ? grid.getTable().getCurrentKey() : null;
    }
}
