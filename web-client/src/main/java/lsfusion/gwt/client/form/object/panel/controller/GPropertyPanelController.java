package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.base.FocusUtils;
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
import lsfusion.gwt.client.form.property.PValue;
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
    private NativeHashMap<GGroupObjectValue, PValue> values;
    private NativeHashMap<GGroupObjectValue, PValue> captions;
    private NativeHashMap<GGroupObjectValue, PValue> loadings;
    private NativeHashMap<GGroupObjectValue, PValue> showIfs;
    private NativeHashMap<GGroupObjectValue, PValue> readOnly;
    private NativeHashMap<GGroupObjectValue, PValue> cellBackgroundValues;
    private NativeHashMap<GGroupObjectValue, PValue> cellForegroundValues;
    private NativeHashMap<GGroupObjectValue, PValue> cellValueElementClasses;
    private NativeHashMap<GGroupObjectValue, PValue> cellCaptionElementClasses;

    private NativeHashMap<GGroupObjectValue, PValue> images;

    private NativeHashMap<GGroupObjectValue, PValue> comments;
    private NativeHashMap<GGroupObjectValue, PValue> cellCommentElementClasses;
    private NativeHashMap<GGroupObjectValue, PValue> placeholders;

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
            columnsPanel.addStyleName("propertyContainerPanel");
            return new ComponentWidget(columnsPanel);
        } else {
            Result<CaptionWidget> captionWidget = alignCaption && property.container.isAlignCaptions() ? new Result<>() : null; // or is tabbed ?
            return new ComponentWidget(addPanelRenderer(GGroupObjectValue.EMPTY, captionWidget), captionWidget != null ? captionWidget.result : null);
        }
   }

    private Pair<List<GGroupObjectValue>, List<GGroupObjectValue>> getDiff() {
        List<GGroupObjectValue> optionsToAdd = new ArrayList<>();
        List<GGroupObjectValue> optionsToRemove = new ArrayList<>();

        NativeHashMap<GGroupObjectValue, Integer> newRenderedColumnKeys = new NativeHashMap<>();
        for (int i = 0; i < columnKeys.size(); i++) {
            GGroupObjectValue columnKey = columnKeys.get(i);
            if (showIfs == null || PValue.getBooleanValue(showIfs.get(columnKey))) {
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
        PValue valueElementClass = null;
        if(cellValueElementClasses != null) {
            valueElementClass = cellValueElementClasses.get(columnKey);
        }
        PValue background = null;
        if (cellBackgroundValues != null) {
            background = cellBackgroundValues.get(columnKey);
        }
        PValue foreground = null;
        if (cellForegroundValues != null) {
            foreground = cellForegroundValues.get(columnKey);
        }
        PValue placeholder = null;
        if(placeholders != null) {
            placeholder = placeholders.get(columnKey);
        }
        renderer.update(values.get(columnKey),
                loadings != null && PValue.getBooleanValue(loadings.get(columnKey)),
                images != null ? PValue.getImageValue(images.get(columnKey)) : null,
                valueElementClass == null ? property.valueElementClass : PValue.getClassStringValue(valueElementClass),
                background == null ? property.getBackground() : PValue.getColorStringValue(background),
                foreground == null ? property.getForeground() : PValue.getColorStringValue(foreground),
                readOnly != null && PValue.getBooleanValue(readOnly.get(columnKey)),
                placeholder == null ? property.placeholder : PValue.getStringValue(placeholder));

        if (captions != null)
            renderer.setCaption(GGridPropertyTable.getDynamicCaption(captions.get(columnKey)));
        if (cellCaptionElementClasses != null)
            renderer.setCaptionElementClass(PValue.getClassStringValue(cellCaptionElementClasses.get(columnKey)));

        if (comments != null)
            renderer.setComment(GGridPropertyTable.getDynamicComment(comments.get(columnKey)));
        if (cellCommentElementClasses != null)
            renderer.setCommentElementClass(PValue.getClassStringValue(cellCommentElementClasses.get(columnKey)));
    }

    public boolean focus(FocusUtils.Reason reason) {
        if (renderers == null || renderers.isEmpty()) {
            return false;
        }

        PanelRenderer toFocus = columnKeys == null ? renderers.firstValue() : renderers.get(columnKeys.get(0));
        if (isShowing(toFocus.getComponent())) {
            toFocus.focus(reason);
            return true;
        }
        return false;
    }

    @Override
    public void setValue(GGroupObjectValue columnKey, PValue value) {
        values.put(columnKey, value);
    }

    @Override
    public void setLoading(GGroupObjectValue columnKey, PValue value) {
        if(loadings == null)
            loadings = new NativeHashMap<>();
        loadings.put(columnKey, value);
    }

    public void setLoadings(NativeHashMap<GGroupObjectValue, PValue> loadingMap) {
        if(loadings == null)
            loadings = new NativeHashMap<>();
        loadings.putAll(loadingMap);
    }

    public void setPropertyValues(NativeHashMap<GGroupObjectValue, PValue> valueMap, boolean updateKeys) {
        if (updateKeys) {
            values.putAll(valueMap);
        } else {
            values = valueMap;
        }
    }

    public void setPropertyCaptions(NativeHashMap<GGroupObjectValue, PValue> captions) {
        this.captions = captions;
    }

    public void setReadOnlyValues(NativeHashMap<GGroupObjectValue, PValue> readOnly) {
        this.readOnly = readOnly;
    }

    public void setShowIfs(NativeHashMap<GGroupObjectValue, PValue> showIfs) {
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

    public void setCellValueElementClasses(NativeHashMap<GGroupObjectValue, PValue> cellValueElementClasses) {
        this.cellValueElementClasses = cellValueElementClasses;
    }

    public void setCellCaptionElementClasses(NativeHashMap<GGroupObjectValue, PValue> cellCaptionElementClasses) {
        this.cellCaptionElementClasses = cellCaptionElementClasses;
    }

    public void setCellBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> cellBackgroundValues) {
        this.cellBackgroundValues = cellBackgroundValues;
    }

    public void setCellForegroundValues(NativeHashMap<GGroupObjectValue, PValue> cellForegroundValues) {
        this.cellForegroundValues = cellForegroundValues;
    }

    public void setImages(NativeHashMap<GGroupObjectValue, PValue> images) {
        this.images = images;
    }

    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GGroupObjectValue fullCurrentKey, PValue value) {
        GGroupObjectValue propertyColumnKey = property.filterColumnKeys(fullCurrentKey);
        if(propertyColumnKey == null)
            return null;
        PanelRenderer panelRenderer = renderers.get(propertyColumnKey);
        if(panelRenderer == null)
            return null;
        return new Pair<>(propertyColumnKey, panelRenderer.setLoadingValue(value));
    }

    public void setPropertyComments(NativeHashMap<GGroupObjectValue, PValue> comments) {
        this.comments = comments;
    }

    public void setCellCommentElementClasses(NativeHashMap<GGroupObjectValue, PValue> cellCommentElementClasses) {
        this.cellCommentElementClasses = cellCommentElementClasses;
    }

    public void setPropertyPlaceholders(NativeHashMap<GGroupObjectValue, PValue> placeholders) {
        this.placeholders = placeholders;
    }
}
