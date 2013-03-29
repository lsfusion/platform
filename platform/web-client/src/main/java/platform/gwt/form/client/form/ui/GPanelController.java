package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.ResizableHorizontalPanel;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.panel.PanelRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPanelController {
    private final GFormLayout formLayout;
    private final GFormController form;

    private final List<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    private final Map<GPropertyDraw, GPropertyController> propertyControllers = new HashMap<GPropertyDraw, GPropertyController>();

    private Object rowBackground;
    private Object rowForeground;

    public GPanelController(GFormController iform, GFormLayout formLayout) {
        this.form = iform;
        this.formLayout = formLayout;
    }

    public void addProperty(GPropertyDraw property) {
        if (property.container != null && !containsProperty(property)) {
            int ins = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), properties);
            properties.add(ins, property);
            propertyControllers.put(property, new GPropertyController(property));
        }
    }

    public void removeProperty(GPropertyDraw property) {
        if (containsProperty(property)) {
            formLayout.remove(property);
            properties.remove(property);
            propertyControllers.remove(property);
        }
    }

    public void update() {
        for (GPropertyDraw property : properties) {
            propertyControllers.get(property).update();
        }
    }

    public boolean isEmpty() {
        return properties.size() == 0;
    }

    public void hide() {
        for (GPropertyController propertyController : propertyControllers.values()) {
            propertyController.getView().setVisible(false);
        }
    }

    public void show() {
        for (GPropertyController propertyController : propertyControllers.values()) {
            propertyController.getView().setVisible(true);
        }
    }

    private boolean visible = true;
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            for (GPropertyController propertyController : propertyControllers.values()) {
                propertyController.getView().setVisible(visible);
            }
        }
    }

    public boolean containsProperty(GPropertyDraw property) {
        return properties.contains(property);
    }

    public void updateRowBackgroundValue(Object color) {
        rowBackground = color;
    }

    public void updateRowForegroundValue(Object color) {
        rowForeground = color;
    }

    public void updateCellBackgroundValues(GPropertyDraw property, Map<GGroupObjectValue, Object> cellBackgroundValues) {
        propertyControllers.get(property).setCellBackgroundValues(cellBackgroundValues);
    }

    public void updateCellForegroundValues(GPropertyDraw property, Map<GGroupObjectValue, Object> cellForegroundValues) {
        propertyControllers.get(property).setCellForegroundValues(cellForegroundValues);
    }

    public void updatePropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
        propertyControllers.get(property).setPropertyValues(valueMap, updateKeys);
    }

    public void updatePropertyCaptions(GPropertyDraw property, Map<GGroupObjectValue, Object> propertyCaptions) {
        propertyControllers.get(property).setPropertyCaptions(propertyCaptions);
    }

    public void updateReadOnlyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> readOnlyValues) {
        propertyControllers.get(property).setReadOnlyValues(readOnlyValues);
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        propertyControllers.get(property).setColumnKeys(columnKeys);
    }

    public boolean focusFirstWidget() {
        if (properties.isEmpty()) {
            return false;
        }

        for (GPropertyDraw property : properties) {
            if (propertyControllers.get(property).focusFirstWidget()) {
                return true;
            }
        }

        return true;
    }

    private class GPropertyController implements DefaultFocusReceiver {
        private boolean addedToLayout = false;
        private boolean columnsUpdated = true;

        private GPropertyDraw property;

        private List<GGroupObjectValue> columnKeys;
        private Map<GGroupObjectValue, PanelRenderer> renderers;

        private Map<GGroupObjectValue, Object> values;

        private Map<GGroupObjectValue, Object> propertyCaptions;
        private Map<GGroupObjectValue, Object> readOnly;
        private Map<GGroupObjectValue, Object> cellBackgroundValues;

        private Map<GGroupObjectValue, Object> cellForegroundValues;
        private ResizableHorizontalPanel renderersPanel;

        public GPropertyController(GPropertyDraw property) {
            this.property = property;
        }

        public Widget getView() {
            return renderersPanel;
        }

        public void update() {
            if (columnsUpdated) {
                if (renderers == null) {
                    renderers = new HashMap<GGroupObjectValue, PanelRenderer>();
                }
                if (renderersPanel == null) {
                    renderersPanel = new ResizableHorizontalPanel();
                }

                List<GGroupObjectValue> columnKeys = this.columnKeys != null ? this.columnKeys : GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                for (GGroupObjectValue columnKey : columnKeys) {
                    if (propertyCaptions == null || propertyCaptions.get(columnKey) != null) {
                        PanelRenderer renderer = renderers.get(columnKey);
                        if (renderer == null) {
                            renderer = property.createPanelRenderer(form, columnKey);
                            renderers.put(columnKey, renderer);
                        }
                    } else {
                        renderers.remove(columnKey);
                    }
                }

                renderersPanel.clear();

                for (GGroupObjectValue columnKey : columnKeys) {
                    PanelRenderer renderer = renderers.get(columnKey);
                    if (renderer != null) {
                        renderersPanel.add(renderer.getComponent());
                        if (renderer.getWidth() != null) {
                            renderersPanel.setWidth(renderer.getWidth());
                        }
                    }
                }

                if (isViewVisible()) {
                    if (!addedToLayout) {
                        formLayout.add(property, getView(), property.container.children.indexOf(property));
                        if (property.defaultComponent) {
                            formLayout.addDefaultComponent(this);
                        }
                        addedToLayout = true;
                    }
                } else {
                    if (addedToLayout) {
                        formLayout.remove(property);
                        addedToLayout = false;
                    }
                }
                columnsUpdated = false;

                if (property.drawAsync && !renderers.isEmpty()) {
                    form.setAsyncView(renderers.get(columnKeys.get(0)));
                }
            }

            for (Map.Entry<GGroupObjectValue, PanelRenderer> e : renderers.entrySet()) {
                updateRenderer(e.getKey(), e.getValue());
            }
        }

        private void updateRenderer(GGroupObjectValue columnKey, PanelRenderer renderer) {
            renderer.setValue(values.get(columnKey));
            renderer.setReadOnly(readOnly != null && readOnly.get(columnKey) != null);

            Object background = rowBackground;
            if (background == null && cellBackgroundValues != null) {
                background = cellBackgroundValues.get(columnKey);
            }
            renderer.updateCellBackgroundValue(background == null ? property.background : background);

            Object foreground = rowForeground;
            if (foreground == null && cellForegroundValues != null) {
                foreground = cellForegroundValues.get(columnKey);
            }
            renderer.updateCellForegroundValue(foreground == null ? property.foreground : foreground);

            if (propertyCaptions != null) {
                renderer.setCaption(property.getDynamicCaption(propertyCaptions.get(columnKey)));
            }
        }

        public boolean focusFirstWidget() {
            if (renderers == null || renderers.isEmpty()) {
                return false;
            }

            PanelRenderer toFocus = columnKeys == null ? renderers.values().iterator().next() : renderers.get(columnKeys.get(0));
            toFocus.focus();

            return true;
        }

        public boolean isViewVisible() {
            return renderersPanel.iterator().hasNext();
        }

        public void setPropertyValues(Map<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
            if (updateKeys) {
                values.putAll(valueMap);
            } else {
                values = valueMap;
            }
        }

        public void setPropertyCaptions(Map<GGroupObjectValue,Object> propertyCaptions) {
            if (columnsUpdated || !GwtSharedUtils.nullEquals(this.propertyCaptions, propertyCaptions)) {
                this.propertyCaptions = propertyCaptions;
                columnsUpdated = true;
            }
        }

        public void setReadOnlyValues(Map<GGroupObjectValue,Object> readOnly) {
            if (columnsUpdated || !GwtSharedUtils.nullEquals(this.readOnly, readOnly)) {
                this.readOnly = readOnly;
                columnsUpdated = true;
            }
        }

        public void setColumnKeys(List<GGroupObjectValue> columnKeys) {
            if (columnsUpdated || !GwtSharedUtils.nullEquals(this.columnKeys, columnKeys)) {
                this.columnKeys = columnKeys;
                columnsUpdated = true;
            }
        }

        public void setCellBackgroundValues(Map<GGroupObjectValue, Object> cellBackgroundValues) {
            this.cellBackgroundValues = cellBackgroundValues;
        }

        public void setCellForegroundValues(Map<GGroupObjectValue, Object> cellForegroundValues) {
            this.cellForegroundValues = cellForegroundValues;
        }

        @Override
        public boolean focus() {
            return focusFirstWidget();
        }
    }
}
