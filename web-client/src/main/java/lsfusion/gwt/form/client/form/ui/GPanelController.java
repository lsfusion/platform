package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPanelController {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();

    private final GFormController form;

    private final Map<GPropertyDraw, GPropertyController> propertyControllers = new HashMap<GPropertyDraw, GPropertyController>();

    private Object rowBackground;
    private Object rowForeground;

    public GPanelController(GFormController iform) {
        this.form = iform;
    }

    public void addProperty(GPropertyDraw property) {
        if (property.container != null && !containsProperty(property)) {
            propertyControllers.put(property, new GPropertyController(property));
        }
    }

    public void removeProperty(GPropertyDraw property) {
        if (containsProperty(property)) {
            GPropertyController propController = propertyControllers.remove(property);
            form.formLayout.remove(property, propController.getView());
        }
    }

    public void update() {
        for (GPropertyController propController : propertyControllers.values()) {
            propController.update();
        }
    }

    public boolean isEmpty() {
        return propertyControllers.size() == 0;
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
        return propertyControllers.containsKey(property);
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

    public void updateShowIfValues(GPropertyDraw property, Map<GGroupObjectValue, Object> showIfs) {
        propertyControllers.get(property).setShowIfs(showIfs);
    }

    public void updateReadOnlyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> readOnlyValues) {
        propertyControllers.get(property).setReadOnlyValues(readOnlyValues);
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        propertyControllers.get(property).setColumnKeys(columnKeys);
    }

    public boolean focusFirstWidget() {
        if (propertyControllers.isEmpty()) {
            return false;
        }

        for (GPropertyController propController : propertyControllers.values()) {
            if (propController.focusFirstWidget()) {
                return true;
            }
        }

        return true;
    }

    public interface RenderersPanel {

        void remove(PanelRenderer renderer);

        void add(PanelRenderer renderer);

        int getWidgetCount();

        Widget asWidget();
    }

    public class GPropertyController {
        private boolean addedToLayout = false;
        private boolean columnsUpdated = true;

        public GPropertyDraw property;

        public Map<GGroupObjectValue, PanelRenderer> renderers;

        public RenderersPanel renderersPanel;

        private List<GGroupObjectValue> columnKeys;
        private Map<GGroupObjectValue, Object> values;
        private Map<GGroupObjectValue, Object> captions;
        private Map<GGroupObjectValue, Object> showIfs;
        private Map<GGroupObjectValue, Object> readOnly;
        private Map<GGroupObjectValue, Object> cellBackgroundValues;
        private Map<GGroupObjectValue, Object> cellForegroundValues;


        public GPropertyController(GPropertyDraw property) {
            this.property = property;
        }

        public Widget getView() {
            return renderersPanel.asWidget();
        }

        public void update() {
            if (columnsUpdated) {
                if (renderers == null) {
                    renderers = new HashMap<GGroupObjectValue, PanelRenderer>();
                }
                if (renderersPanel == null) {
                    renderersPanel = layoutImpl.createRenderersPanel();
                }

                List<GGroupObjectValue> columnKeys = this.columnKeys != null ? this.columnKeys : GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
                for (GGroupObjectValue columnKey : columnKeys) {
                    if (showIfs == null || showIfs.get(columnKey) != null) {
                        PanelRenderer renderer = renderers.get(columnKey);
                        if (renderer == null) {
                            renderer = property.createPanelRenderer(form, columnKey);
                            renderers.put(columnKey, renderer);
                        }
                    } else {
                        PanelRenderer renderer = renderers.remove(columnKey);
                        if (renderer != null) {
                            renderersPanel.remove(renderer);
                        }

                    }
                }

                for (GGroupObjectValue columnKey : columnKeys) {
                    PanelRenderer renderer = renderers.get(columnKey);
                    if (renderer != null && renderer.getComponent().getParent() != renderersPanel) {
                        renderersPanel.add(renderer);
                    }
                }

                if (renderersPanel.getWidgetCount() > 0) {
                    if (!addedToLayout) {
                        form.formLayout.add(property, renderersPanel.asWidget(), new DefaultFocusReceiver() {
                            @Override
                            public boolean focus() {
                                return focusFirstWidget();
                            }
                        });

                        //донастройка рендереров в лэйауте
                        for (PanelRenderer renderer : renderers.values()) {
                            renderer.setupLayout(this);
                        }

                        addedToLayout = true;
                    }
                } else {
                    if (addedToLayout) {
                        form.formLayout.remove(property, renderersPanel.asWidget());
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

            if (captions != null) {
                renderer.setCaption(property.getDynamicCaption(captions.get(columnKey)));
            }
        }

        public boolean focusFirstWidget() {
            if (renderers == null || renderers.isEmpty()) {
                return false;
            }

            PanelRenderer toFocus = columnKeys == null ? renderers.values().iterator().next() : renderers.get(columnKeys.get(0));
            if (GwtClientUtils.isVisible(toFocus.getComponent())) {
                toFocus.focus();
                return true;
            }
            return false;
        }

        public void setPropertyValues(Map<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
            if (updateKeys) {
                values.putAll(valueMap);
            } else {
                values = valueMap;
            }
        }

        public void setPropertyCaptions(Map<GGroupObjectValue,Object> captions) {
            this.captions = captions;
        }

        public void setReadOnlyValues(Map<GGroupObjectValue,Object> readOnly) {
            this.readOnly = readOnly;
        }

        public void setShowIfs(Map<GGroupObjectValue,Object> showIfs) {
            if (columnsUpdated || !GwtSharedUtils.nullEquals(this.showIfs, showIfs)) {
                this.showIfs = showIfs;
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
    }
}
