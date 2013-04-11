package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.client.form.ui.container.GAbstractFormContainer;
import platform.gwt.form.client.form.ui.toolbar.GCalculateSumButton;
import platform.gwt.form.client.form.ui.toolbar.GCountQuantityButton;
import platform.gwt.form.client.form.ui.toolbar.GToolbarButton;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GFormChanges;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GIntegralType;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
import platform.gwt.form.shared.view.reader.*;

import java.util.*;

import static platform.gwt.base.shared.GwtSharedUtils.containsAny;

public class GGroupObjectController extends GAbstractGroupObjectController {
    public GGroupObject groupObject;

    private GGridController grid;
    private GShowTypeView showTypeView;

    private GClassViewType classViewType = GClassViewType.GRID;

    // пустая панель внизу контейнера группы, которая расширяется при спрятанном гриде, прижимая тулбар и панельный контейнер кверху
    private SimplePanel blankElement = new SimplePanel();

    private GCountQuantityButton quantityButton;
    private GCalculateSumButton sumButton;

    public GGroupObjectController(GFormController iformController, GGroupObject igroupObject) {
        super(iformController, igroupObject == null ? null : igroupObject.toolbar);
        groupObject = igroupObject;

        if (groupObject != null) {
            grid = new GGridController(groupObject.grid, formController, this);
            grid.addToLayout(getFormLayout());

            GContainer gridContainer = groupObject.grid.container;
            getFormLayout().setTableCellSize(gridContainer.container, gridContainer, "100%", true);
            getFormLayout().setTableCellSize(gridContainer.container, gridContainer, "100%", false);

            showTypeView = new GShowTypeView(formController, groupObject);
            showTypeView.addToLayout(getFormLayout());
            showTypeView.setBanClassViews(groupObject.banClassView);

            GAbstractFormContainer gridParentParent = getFormLayout().getFormContainer(groupObject.grid.container.container);
            if (gridParentParent != null) {
                gridParentParent.addDirectly(blankElement);
            }

            configureToolbar();
        }
    }

    private void configureToolbar() {
        addFilterButton();
        if (filter != null && grid != null) {
            addToToolbar(GwtClientUtils.createHorizontalStrut(5));
        }

        if (groupObject.toolbar.showGroupChange) {
            addToToolbar(new GToolbarButton("groupchange.png", "Групповая корректировка (F12)") {
                @Override
                public void addListener() {
                    addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            grid.getTable().editCurrentCell(GEditBindingMap.GROUP_CHANGE);
                        }
                    });
                }
            });
        }

        if (groupObject.toolbar.showCountQuantity) {
            quantityButton = new GCountQuantityButton() {
                public void addListener() {
                    addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            formController.countRecords(groupObject);
                        }
                    });
                }
            };
            addToToolbar(quantityButton);
        }

        if (groupObject.toolbar.showCalculateSum) {
            sumButton = new GCalculateSumButton() {
                @Override
                public void addListener() {
                    addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            GPropertyDraw property = getSelectedProperty();
                            if (property.baseType instanceof GIntegralType) {
                                formController.calculateSum(groupObject, getSelectedProperty(), grid.getTable().getCurrentColumnKey());
                            } else {
                                showSum(null, property);
                            }
                        }
                    });
                }
            };
            addToToolbar(sumButton);
        }

        addToToolbar(GwtClientUtils.createHorizontalStrut(5));

        if (groupObject.toolbar.showPrintGroupButton) {
            addToToolbar(new GToolbarButton("reportbw.png", "Распечатать таблицу") {
                @Override
                public void addListener() {
                    addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            formController.runSingleGroupReport(groupObject.ID, false);
                        }
                    });
                }
            });
        }

        if (groupObject.toolbar.showPrintGroupXlsButton) {
            addToToolbar(new GToolbarButton("excelbw.png", "Экспорт в xls") {
                @Override
                public void addListener() {
                    addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            formController.runSingleGroupReport(groupObject.ID, true);
                        }
                    });
                }
            });
        }
    }

    public void showRecordQuantity(int quantity) {
        quantityButton.showPopup(quantity);
    }

    public void showSum(Number sum, GPropertyDraw property) {
        sumButton.showPopup(sum, property);
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
                            (fc.classViews.containsKey(groupObject) || containsAny(changedGroups, property.columnGroupObjects))) {
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
    public void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateReadOnlyValues(property, values);
        } else if (grid != null) {
            grid.updateReadOnlyValues(property, values);
        }
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
        }
    }

    private void removeProperty(GPropertyDraw property) {
        panel.removeProperty(property);
        if (grid != null) {
            grid.getTable().removeProperty(property);
        }
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
    }

    private void addPanelProperty(GPropertyDraw property) {
        if (grid != null) {
            grid.getTable().removeProperty(property);
        }
        panel.addProperty(property);
    }

    private void updateGrid() {
        if (groupObject != null) {
            grid.update();
            toolbar.setVisible(grid.isVisible());
            if (filter != null) {
                filter.setVisible(grid.isVisible());
            }

            GContainer gridContainer = groupObject.grid.container;
            if (grid.isVisible()) {
                getFormLayout().setTableCellSize(gridContainer.container, gridContainer, "100%", false);
                getFormLayout().setTableCellSize(gridContainer.container, blankElement, "auto", false);
            } else {
                getFormLayout().setTableCellSize(gridContainer.container, gridContainer, "auto", false);
                getFormLayout().setTableCellSize(gridContainer.container, blankElement, "100%", false);
            }
            if (showTypeView != null) {
                showTypeView.setClassView(classViewType);
            }
        }
    }

    private void update() {
        updateGrid();
        panel.update();
        panel.setVisible(classViewType != GClassViewType.HIDE);
    }

    void restoreScrollPosition() {
        if (grid != null) {
            grid.restoreScrollPosition();
        }
    }

    public boolean isInGridClassView() {
        return classViewType == GClassViewType.GRID;
    }

    public boolean isGridEmpty() {
        return grid == null || grid.getTable().isEmpty();
    }

    public GGroupObjectValue getCurrentKey() {
        GGroupObjectValue result = null;
        if (grid != null) {
            result = grid.getTable().getCurrentKey();
        }
        return result == null ? GGroupObjectValue.EMPTY : result;
    }

    @Override
    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        grid.changeOrder(property, modiType);
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        return groupObject;
    }

    @Override
    public List<GPropertyDraw> getGroupObjectProperties() {
        ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
        for (GPropertyDraw property : formController.getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }
        return properties;
    }

    @Override
    public GPropertyDraw getSelectedProperty() {
        GPropertyDraw defaultProperty = groupObject.filterProperty;
        return defaultProperty != null
                ? defaultProperty
                : grid.getCurrentProperty();
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property) {
        return grid.getSelectedValue(property);
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

    @Override
    protected boolean showFilter() {
        return groupObject != null && groupObject.filter.visible;
    }

    @Override
    protected void changeFilter(List<GPropertyFilter> conditions) {
        formController.changeFilter(groupObject, conditions);
    }

    @Override
    public void setFilterVisible(boolean visible) {
        if (isInGridClassView()) {
            super.setFilterVisible(visible);
        }
    }

    public void selectProperty(GPropertyDraw propertyDraw) {
        grid.selectProperty(propertyDraw);
    }
}
