package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.form2.shared.view.GContainer;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GFormChanges;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form2.shared.view.reader.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GGroupObjectController implements GGroupObjectLogicsSupplier {
    public GGroupObject groupObject;
    private GFormLayout formLayout;
    private GFormController formController;

    public GGridController grid;
    public GPanelController panel;
    public GToolbarPanel panelToolbar;
    private GToolbarPanel gridToolbar;
    private GShowTypeView showTypeView;

    private GClassViewType classViewType = GClassViewType.HIDE;

    private HashSet<GPropertyDraw> panelProperties = new HashSet<GPropertyDraw>();

    public GGroupObjectController(GFormController iformController, GGroupObject igroupObject, GFormLayout iformLayout) {
        groupObject = igroupObject;
        formLayout = iformLayout;
        formController = iformController;

        gridToolbar = new GToolbarPanel();
        gridToolbar.addStyleName("gridToolbar");

        panel = new GPanelController(formController, formLayout);

        panelToolbar = new GToolbarPanel();
        panelToolbar.addStyleName("panelToolbar");

        if (groupObject != null) {
            grid = new GGridController(groupObject.grid, formController, this);
            grid.addToLayout(formLayout);

            GContainer gridContainer = groupObject.grid.container;
            formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", true);
            formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", false);

            formLayout.add(gridContainer, panelToolbar);

            showTypeView = new GShowTypeView(formController, groupObject) {
                protected void needToBeShown() {
                    showViews();
                }

                protected void needToBeHidden() {
                    hideViews();
                }
            };
            showTypeView.setBanClassViews(groupObject.banClassView);
        }
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

        for (GPropertyReader propertyReader : fc.properties.keySet()) {
            if (propertyReader instanceof GPropertyDraw) {
                GPropertyDraw property = (GPropertyDraw) propertyReader;
                if (property.groupObject == groupObject && !fc.updateProperties.contains(property)) {
                    if (fc.panelProperties.contains(property)) {
                        addPanelProperty(property);
                    } else {
                        addGridProperty(property);
                    }
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

        for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            GPropertyReader propertyReader = readProperty.getKey();
            if (formController.getGroupObject(propertyReader.getGroupObjectID()) == groupObject) {
                propertyReader.update(this, readProperty.getValue(), fc.updateProperties.contains(propertyReader));
            }
        }

        update();
    }

    @Override
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.setPropertyValues(property, values, updateKeys);
        } else {
            grid.getTable().setPropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, values);
        } else {
            grid.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, values);
        } else {
            grid.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, values);
        } else {
            grid.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        if (isInGridClassView()) {
            grid.updateRowBackgroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowBackgroundValue(values.values().iterator().next());
            }
        }
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        if (isInGridClassView()) {
            grid.updateRowForegroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowForegroundValue(values.values().iterator().next());
            }
        }
    }

    public boolean isShowTypeVisible() {
        return showTypeView.needToBeVisible();
    }

    public void setClassView(GClassViewType newClassView) {
        if (newClassView != null && newClassView != classViewType) {
            classViewType = newClassView;

            if (showTypeView != null && showTypeView.setClassView(newClassView)) {
                updateToolbar();
//                formLayout.adjustSizes();
            }
        }
    }

    private void updateToolbar() {
        if (groupObject != null) {
            GContainer gridContainer = groupObject.grid.container;
            if (classViewType == GClassViewType.GRID) {
                panelToolbar.removeComponent(showTypeView);
                gridToolbar.addComponent(showTypeView);
                grid.show();
                formLayout.setTableCellSize(gridContainer.container, panelToolbar, "auto", false);
                formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", false);
            } else {
//                formLayout.add(groupObject.showType, showTypeView);
                for (Widget widget : gridToolbar) {
                    gridToolbar.removeComponent(widget);
                    panelToolbar.addComponent(widget);
                }
                panelToolbar.addComponent(showTypeView);
                grid.hide();
                formLayout.setTableCellSize(gridContainer.container, panelToolbar, "100%", false);
                formLayout.setTableCellSize(gridContainer.container, gridContainer, "auto", false);
            }
        }
    }

    private void hideViews() {
        grid.hide();
        panel.hide();
        gridToolbar.removeComponent(showTypeView);
        panelToolbar.addComponent(showTypeView);
        for (Widget component : panelToolbar) {
            if (component != showTypeView) {
                component.setVisible(false);
            }
        }
    }

    private void showViews() {
        grid.show();
        panel.show();
        for (Widget component : panelToolbar) {
            component.setVisible(true);
        }
    }

    public GToolbarPanel getGridToolbar() {
        return gridToolbar;
    }

    private void removeProperty(GPropertyDraw property) {
        panel.removeProperty(property);
        if (grid != null) {
            grid.getTable().removeProperty(property);
        }
        panelProperties.remove(property);
    }

    private void addGridProperty(GPropertyDraw property) {
        if (grid != null) {
            grid.getTable().addProperty(property);
        }
        panel.removeProperty(property);
        panelProperties.remove(property);
    }

    private void addPanelProperty(GPropertyDraw property) {
        if (grid != null) {
            grid.getTable().removeProperty(property);
        }
        panel.addProperty(property);
        panelProperties.add(property);
    }

    public boolean hasPanelProperty(GPropertyDraw property) {
        return panelProperties.contains(property);
    }

    public void addFilterComponent(Widget filterWidget) {
        gridToolbar.addComponent(filterWidget);
    }

    public void addPanelFilterComponent(Widget filterWidget) {
        panelToolbar.addComponent(filterWidget);
    }

    private void update() {
        if (grid != null) {
            grid.update();
        }
        panel.update();

        gridToolbar.setVisible(!gridToolbar.isEmpty());
        panelToolbar.setVisible(!panelToolbar.isEmpty());
    }

    public boolean isInGridClassView() {
        return classViewType == GClassViewType.GRID;
    }

    public GGroupObjectValue getCurrentKey() {
        GGroupObjectValue result = null;
        if (grid != null) {
            result = grid.getTable().getCurrentKey();
        }
        return result == null ? new GGroupObjectValue() : result;
    }

    public void modifyGroupObject(GGroupObjectValue key, boolean add) {
        assert classViewType == GClassViewType.GRID;

        grid.modifyGridObject(key, add);
    }
}
