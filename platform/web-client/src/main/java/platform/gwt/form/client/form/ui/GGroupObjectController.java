package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.client.form.ui.container.GAbstractFormContainer;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GFormChanges;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.reader.*;

import java.util.*;

public class GGroupObjectController implements GGroupObjectLogicsSupplier {
    public GGroupObject groupObject;
    private GFormLayout formLayout;
    private GFormController formController;

    private GGridController grid;
    private GPanelController panel;
    private GShowTypeView showTypeView;

    private GClassViewType classViewType = GClassViewType.HIDE;

    private HashSet<GPropertyDraw> panelProperties = new HashSet<GPropertyDraw>();

    // пустая панель внизу контейнера группы, которая расширяется при спрятанном гриде, прижимая тулбар и панельный контейнер кверху
    private SimplePanel blankElement = new SimplePanel();

    public GGroupObjectController(GFormController iformController, GGroupObject igroupObject, GFormLayout iformLayout) {
        groupObject = igroupObject;
        formLayout = iformLayout;
        formController = iformController;

        panel = new GPanelController(formController, formLayout);

        if (groupObject != null) {
            grid = new GGridController(groupObject.grid, formController, this);
            grid.addToLayout(formLayout);

            GContainer gridContainer = groupObject.grid.container;
            formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", true);
            formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", false);

            showTypeView = new GShowTypeView(formController, groupObject) {
                protected void needToBeShown() {
                    showViews();
                }

                protected void needToBeHidden() {
                    hideViews();
                }
            };
            showTypeView.addToLayout(formLayout);
            showTypeView.setBanClassViews(groupObject.banClassView);

            GAbstractFormContainer gridParentParent = formLayout.getFormContainer(groupObject.grid.container.container);
            if (gridParentParent != null) {
                gridParentParent.addDirectly(blankElement);
            }
        }
    }

    public void processFormChanges(GFormChanges fc, HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects, HashSet<GGroupObject> changedGroups) {
        for (GPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        GClassViewType classViewValue = fc.classViews.get(groupObject);
        if (classViewValue != null) {
            setClassView(classViewValue);
        }

        for (GPropertyReader propertyReader : fc.properties.keySet()) {
            if (propertyReader instanceof GPropertyDraw) {
                GPropertyDraw property = (GPropertyDraw) propertyReader;
                if (property.groupObject == groupObject && !fc.updateProperties.contains(property)) {
                    addProperty(property, fc.panelProperties.contains(property));

                    if (property.columnGroupObjects != null &&
                            (fc.classViews.containsKey(groupObject) || GwtSharedUtils.containsAny(changedGroups, property.columnGroupObjects))) {
                        LinkedHashMap<GGroupObject, List<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<GGroupObject, List<GGroupObjectValue>>();
                        for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                            List<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                            if (columnGroupKeys != null) {
                                groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                            }
                        }

                        updateDrawColumnKeys(property, GGroupObject.mergeGroupValues(groupColumnKeys));
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

    private void updateDrawColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        if (panel.containsProperty(property)) {
            panel.updateColumnKeys(property, columnKeys);
        } else if (grid != null) {
            grid.updateColumnKeys(property, columnKeys);
        }
    }

    @Override
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, updateKeys);
        } else if (grid != null) {
            grid.getTable().updatePropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, values);
        } else if (grid != null) {
            grid.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, values);
        } else if (grid != null) {
            grid.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, values);
        } else if (grid != null) {
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

    public void setClassView(GClassViewType newClassView) {
        if (newClassView != null && newClassView != classViewType) {
            classViewType = newClassView;

            if (showTypeView != null && showTypeView.setClassView(newClassView)) {
                updateToolbar();
            }
        }
    }

    private void updateToolbar() {
        if (groupObject != null) {
            GContainer gridContainer = groupObject.grid.container;
            if (classViewType == GClassViewType.GRID) {
                grid.show();
                formLayout.setTableCellSize(gridContainer.container, gridContainer, "100%", false);
                formLayout.setTableCellSize(gridContainer.container, blankElement, "auto", false);
            } else {
                grid.hide();
                formLayout.setTableCellSize(gridContainer.container, gridContainer, "auto", false);
                formLayout.setTableCellSize(gridContainer.container, blankElement, "100%", false);
            }
        }
    }

    private void hideViews() {
        grid.hide();
        panel.hide();
    }

    private void showViews() {
        grid.show();
        panel.show();
    }

    private void removeProperty(GPropertyDraw property) {
        panel.removeProperty(property);
        if (grid != null) {
            grid.getTable().removeProperty(property);
        }
        panelProperties.remove(property);
    }

    private void addProperty(GPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            addPanelProperty(property);
        } else {
            addGridProperty(property);
        }
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

    public void addFilterComponent(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.add(filterGroup, filterWidget);
    }

    private void update() {
        if (grid != null) {
            grid.update();
        }
        panel.update();
    }

    public void rememberScrollPosition() {
        if (grid != null) {
            grid.rememberScrollPosition();
        }
    }

    public void preparePendingState() {
        if (grid != null) {
            grid.preparePendingState();
        }
    }

    public void applyPendingState() {
        if (grid != null) {
            grid.applyPendingState();
        }
    }

    public void relayoutTable() {
        grid.relayoutTable();
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

    @Override
    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        grid.changeOrder(property, modiType);
    }

    public void modifyGroupObject(GGroupObjectValue key, boolean add) {
        assert classViewType == GClassViewType.GRID;

        grid.modifyGridObject(key, add);
    }

    public boolean focusFirstWidget() {
        if (grid != null && !grid.getTable().isEmpty()) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    grid.getTable().setFocus(true);
                }
            });
            return true;
        }

        return panel.focusFirstWidget();
    }
}
