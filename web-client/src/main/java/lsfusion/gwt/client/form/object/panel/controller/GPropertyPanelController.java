package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class GPropertyPanelController implements ActionOrPropertyValueController {
    private boolean columnsUpdated = true;

    public GPropertyDraw property;

    private final GFormController form;

    private NativeHashMap<GGroupObjectValue, Integer> renderedColumnKeys;
    private NativeHashMap<GGroupObjectValue, PanelRenderer> renderers;

    public SizedFlexPanel columnsPanel;

    private ArrayList<GGroupObjectValue> columnKeys;
    // it doesn't make sense to make this maps Native since they come from server and are built anyway
    private NativeHashMap<GGroupObjectValue, Object> values;
    private NativeHashMap<GGroupObjectValue, Object> captions;
    private NativeHashMap<GGroupObjectValue, Object> loadings;
    private NativeHashMap<GGroupObjectValue, Object> showIfs;
    private NativeHashMap<GGroupObjectValue, Object> readOnly;
    private NativeHashMap<GGroupObjectValue, Object> cellBackgroundValues;
    private NativeHashMap<GGroupObjectValue, Object> cellForegroundValues;

    private NativeHashMap<GGroupObjectValue, Object> images;

    public GPropertyPanelController(GPropertyDraw property, GFormController form) {
        this.property = property;
        this.form = form;
        renderedColumnKeys = new NativeHashMap<>();
        renderers = new NativeHashMap<>();
    }

    private boolean needColumnsPanel() {
        return property.hasColumnGroupObjects() || property.hide;
    }

    public ComponentWidget initView() {
        boolean alignCaption = property.isAlignCaption();
        if(needColumnsPanel()) {
            assert !alignCaption;

            columnsPanel = new SizedFlexPanel(property.panelColumnVertical);
            columnsPanel.setStyleName("propertyContainerPanel");
            return new ComponentWidget(columnsPanel);
        } else {
            Result<CaptionWidget> captionWidget = alignCaption && property.container.isAlignCaptions() ? new Result<>() : null;
            return new ComponentWidget(addPanelRenderer(GGroupObjectValue.EMPTY, captionWidget), captionWidget != null ? captionWidget.result : null);
        }
   }

    private Pair<List<GGroupObjectValue>, List<GGroupObjectValue>> getDiff() {
        List<GGroupObjectValue> optionsToAdd = new ArrayList<>();
        List<GGroupObjectValue> optionsToRemove = new ArrayList<>();

        NativeHashMap<GGroupObjectValue, Integer> newRenderedColumnKeys = new NativeHashMap<>();
        for (int i = 0; i < columnKeys.size(); i++) {
            GGroupObjectValue columnKey = columnKeys.get(i);
            if (showIfs == null || showIfs.get(columnKey) != null) {
                Integer oldColumnKeyOrder = renderedColumnKeys.remove(columnKey);
                if (oldColumnKeyOrder != null) {
                    if (i != oldColumnKeyOrder) {
                        optionsToRemove.add(columnKey);
                        optionsToAdd.add(columnKey);
                    }
                } else {
                    optionsToAdd.add(columnKey);
                }
                newRenderedColumnKeys.put(columnKey, i);
            }
        }

        renderedColumnKeys.foreachKey(optionsToRemove::add);

        renderedColumnKeys = newRenderedColumnKeys;

        return new Pair<>(optionsToAdd, optionsToRemove);
    }

    public void update() {
        if (columnsUpdated) {
            boolean hide = property.hide;
            if (!hide || property.hasKeyBinding()) {

                Pair<List<GGroupObjectValue>, List<GGroupObjectValue>> pair = getDiff();
                List<GGroupObjectValue> optionsToAdd = pair.first;
                List<GGroupObjectValue> optionsToRemove = pair.second;

                // removing old renderers
                optionsToRemove.forEach(columnKey -> {
                    PanelRenderer renderer = removePanelRenderer(columnKey);

                    if (!hide) {
                        columnsPanel.removeSized(renderer.getComponent());
                    }
                });

                //adding new renderers
                optionsToAdd.forEach(columnKey -> {
                    SizedWidget component = addPanelRenderer(columnKey, null);

                    if (!hide) {
                        component.addFill(columnsPanel, renderedColumnKeys.get(columnKey));
                    }
                });
            }

            columnsUpdated = false;
        }

        renderers.foreachEntry(this::updateRenderer);
    }

    public SizedWidget addPanelRenderer(GGroupObjectValue columnKey, Result<CaptionWidget> caption) {
        PanelRenderer newRenderer = property.createPanelRenderer(form, GPropertyPanelController.this, columnKey, caption);
        SizedWidget component = newRenderer.getSizedWidget();
        newRenderer.bindingEventIndices = form.addPropertyBindings(property, newRenderer::onBinding, component.widget);
        renderers.put(columnKey, newRenderer);
        return component;
    }

    public PanelRenderer removePanelRenderer(GGroupObjectValue columnKey) {
        PanelRenderer renderer = renderers.remove(columnKey);
        form.removePropertyBindings(renderer.bindingEventIndices);
        return renderer;
    }

    private void updateRenderer(GGroupObjectValue columnKey, PanelRenderer renderer) {
        Object background = null;
        if (cellBackgroundValues != null) {
            background = cellBackgroundValues.get(columnKey);
        }
        Object foreground = null;
        if (cellForegroundValues != null) {
            foreground = cellForegroundValues.get(columnKey);
        }
        renderer.update(values.get(columnKey),
                loadings != null && loadings.get(columnKey) != null,
                images != null ? images.get(columnKey) : null,
                background == null ? property.background : background,
                foreground == null ? property.foreground : foreground,
                readOnly != null && readOnly.get(columnKey) != null);

        if (captions != null) {
            renderer.setCaption(GGridPropertyTable.getPropertyCaption(captions, property, columnKey));
        }
    }

    public boolean focusFirstWidget() {
        if (renderers == null || renderers.isEmpty()) {
            return false;
        }

        PanelRenderer toFocus = columnKeys == null ? renderers.firstValue() : renderers.get(columnKeys.get(0));
        if (isShowing(toFocus.getComponent())) {
            toFocus.focus();
            return true;
        }
        return false;
    }

    @Override
    public void setValue(GGroupObjectValue columnKey, Object value) {
        values.put(columnKey, value);
    }

    @Override
    public void setLoading(GGroupObjectValue columnKey, Object value) {
        if(loadings == null)
            loadings = new NativeHashMap<>();
        loadings.put(columnKey, value);
    }

    public void setLoadings(NativeHashMap<GGroupObjectValue, Object> loadingMap) {
        if(loadings == null)
            loadings = new NativeHashMap<>();
        loadings.putAll(loadingMap);
    }

    public void setPropertyValues(NativeHashMap<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
        if (updateKeys) {
            values.putAll(valueMap);
        } else {
            values = valueMap;
        }
    }

    public void setPropertyCaptions(NativeHashMap<GGroupObjectValue, Object> captions) {
        this.captions = captions;
    }

    public void setReadOnlyValues(NativeHashMap<GGroupObjectValue, Object> readOnly) {
        this.readOnly = readOnly;
    }

    public void setShowIfs(NativeHashMap<GGroupObjectValue, Object> showIfs) {
        if (!GwtSharedUtils.nullEquals(this.showIfs, showIfs)) {
            this.showIfs = showIfs;

            columnsUpdated = needColumnsPanel();
        }
    }

    public void setColumnKeys(ArrayList<GGroupObjectValue> columnKeys) {
        if (!GwtSharedUtils.nullEquals(this.columnKeys, columnKeys)) {
            this.columnKeys = columnKeys;

            columnsUpdated = needColumnsPanel();
        }
    }

    public void setCellBackgroundValues(NativeHashMap<GGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues = cellBackgroundValues;
    }

    public void setCellForegroundValues(NativeHashMap<GGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues = cellForegroundValues;
    }

    public void setImages(NativeHashMap<GGroupObjectValue, Object> images) {
        this.images = images;
    }

    public Pair<GGroupObjectValue, Object> setLoadingValueAt(GGroupObjectValue fullCurrentKey, Object value) {
        GGroupObjectValue propertyColumnKey = property.filterColumnKeys(fullCurrentKey);
        if(propertyColumnKey == null)
            return null;
        PanelRenderer panelRenderer = renderers.get(propertyColumnKey);
        if(panelRenderer == null)
            return null;
        return new Pair<>(propertyColumnKey, panelRenderer.setLoadingValue(value));
    }
}
