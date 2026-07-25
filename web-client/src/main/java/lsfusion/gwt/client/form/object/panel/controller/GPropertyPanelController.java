package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.view.MainFrame.v5;

public class GPropertyPanelController implements ActionOrPropertyValueController {
    private boolean rendererKeysUpdated = true;

    public GPropertyDraw property;

    private final GFormController form;

    private NativeHashMap<GGroupObjectValue, Integer> renderedKeys;
    private NativeHashMap<GGroupObjectValue, PanelRenderer> renderers;

    // what a renderer key means for this property and where its renderers go: columns of the property's own panel, or
    // rows of a grid a CUSTOM REACT view draws - and it is the panel controller that owns this one which says which,
    // because the placement state (a column panel, a row's host) is its own
    private final GAbstractPanelController panelController;

    private ArrayList<GGroupObjectValue> rendererKeys;
    // it doesn't make sense to make this maps Native since they come from server and are built anyway
    private NativeHashMap<GGroupObjectValue, PValue> values;
    private NativeHashMap<GGroupObjectValue, PValue> loadings;
    private NativeHashMap<GGroupObjectValue, PValue> showIfs;

    // every ATTRIBUTE the server delivers, kept under the reader that delivered it instead of in a field per attribute.
    // Keeping the reader is what lets the lookup ASK it which axis it was read over (readAttribute) rather than restate
    // that rule here - the value itself, its loading flag and showIf stay apart above: they are not attributes.
    private final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, PValue>> attributeValues = new NativeSIDMap<>();

    public GPropertyPanelController(GPropertyDraw property, GFormController form, GAbstractPanelController panelController) {
        this.property = property;
        this.form = form;
        this.panelController = panelController;
        renderedKeys = new NativeHashMap<>();
        renderers = new NativeHashMap<>();
    }

    private boolean formView; // whether the form laid something out for this property (see initView)

    public boolean hasFormView() {
        return formView;
    }

    public ComponentWidget initView() {
        boolean alignCaption = property.isAlignCaption();
        if(!panelController.isSingleRenderer(property)) {
            assert !alignCaption;

            Widget formWidget = panelController.getFormWidget(property);
            formView = formWidget != null;
            return formView ? new ComponentWidget(formWidget) : null; // null: the form lays out nothing here
        } else {
            formView = true;
            Result<CaptionWidget> captionWidget = alignCaption && property.container.isAlignCaptions() ? new Result<>() : null; // or is tabbed ?
            return new ComponentWidget(addPanelRenderer(GGroupObjectValue.EMPTY, captionWidget), captionWidget != null ? captionWidget.result : null);
        }
   }

    private Pair<List<GGroupObjectValue>, List<GGroupObjectValue>> getDiff() {
        List<GGroupObjectValue> optionsToAdd = new ArrayList<>();
        List<GGroupObjectValue> optionsToRemove = new ArrayList<>();

        NativeHashMap<GGroupObjectValue, Integer> newRenderedColumnKeys = new NativeHashMap<>();
        for (int i = 0; i < rendererKeys.size(); i++) {
            GGroupObjectValue rendererKey = rendererKeys.get(i);
            if (showIfs == null || PValue.getBooleanValue(showIfs.get(panelController.getColumnKey(rendererKey)))) {
                Integer oldColumnKeyOrder = renderedKeys.remove(rendererKey);
                if (oldColumnKeyOrder != null) {
                    if (panelController.recreateRendererOnMove(oldColumnKeyOrder, i)) {
                        optionsToRemove.add(rendererKey);
                        optionsToAdd.add(rendererKey);
                    }
                } else {
                    optionsToAdd.add(rendererKey);
                }
                newRenderedColumnKeys.put(rendererKey, newRenderedColumnKeys.size());
            }
        }

        renderedKeys.foreachKey(optionsToRemove::add);

        renderedKeys = newRenderedColumnKeys;

        return new Pair<>(optionsToAdd, optionsToRemove);
    }

    public void update() {
        if (rendererKeysUpdated) {
            if (panelController.shouldCreateRenderers(property)) {

                Pair<List<GGroupObjectValue>, List<GGroupObjectValue>> pair = getDiff();
                List<GGroupObjectValue> optionsToAdd = pair.first;
                List<GGroupObjectValue> optionsToRemove = pair.second;

                // removing old renderers
                optionsToRemove.forEach(rendererKey ->
                        panelController.removeRenderer(rendererKey, property, removePanelRenderer(rendererKey)));

                //adding new renderers
                optionsToAdd.forEach(rendererKey ->
                        panelController.addRenderer(rendererKey, property, addPanelRenderer(rendererKey, null), renderedKeys.get(rendererKey)));
            }

            rendererKeysUpdated = false;
        }

        renderers.foreachEntry(this::updateRenderer);
    }

    // the renderer drawn for this key, or null. It is the only record of it: whoever places renderers asks here rather
    // than keeping a second map of its own
    public PanelRenderer getRenderer(GGroupObjectValue rendererKey) {
        return renderers.get(rendererKey);
    }

    // take every renderer out and destroy it. NOT through the diff: the diff is skipped entirely when the panel says
    // no renderers should exist right now, which is exactly the state a hidden property with renderers left over is in
    // the form is closing: unhook every renderer from what outlives the form (the static color-theme listener list,
    // a tippy) - and nothing more. No layout surgery: the form's whole DOM subtree, parks included, is discarded with it.
    public void destroyRenderers() {
        renderers.foreachValue(PanelRenderer::destroy);
    }

    public void removeAllRenderers() {
        List<GGroupObjectValue> existing = new ArrayList<>();
        renderers.foreachKey(existing::add);
        for (GGroupObjectValue rendererKey : existing)
            panelController.removeRenderer(rendererKey, property, removePanelRenderer(rendererKey));

        renderedKeys = new NativeHashMap<>();
        rendererKeys = new ArrayList<>();
    }

    public ComponentViewWidget addPanelRenderer(GGroupObjectValue rendererKey, Result<CaptionWidget> caption) {
        PanelRenderer newRenderer = property.createPanelRenderer(form, GPropertyPanelController.this, panelController.getColumnKey(rendererKey), panelController.getRowKey(rendererKey), caption);
        ComponentViewWidget component = newRenderer.getComponentViewWidget();
        if (panelController.shouldRegisterBindings())
            newRenderer.bindingEventIndices = form.addPropertyBindings(property, newRenderer::onBinding, component.getShowingWidget());
        renderers.put(rendererKey, newRenderer);
        return component;
    }

    public ComponentViewWidget removePanelRenderer(GGroupObjectValue rendererKey) {
        PanelRenderer renderer = renderers.remove(rendererKey);
        if (renderer.bindingEventIndices != null) // a per-row renderer registers none, so there are none to take back
            form.removePropertyBindings(renderer.bindingEventIndices);
        ComponentViewWidget componentViewWidget = renderer.getComponentViewWidget();
        renderer.destroy(); // after the widget is read out: a dropped renderer is otherwise kept alive by its tippy and by MainFrame's color-theme listener list
        return componentViewWidget;
    }

    // an attribute's values, or null when this property has no such reader (nothing was ever delivered for it)
    private NativeHashMap<GGroupObjectValue, PValue> attribute(GPropertyReader reader) {
        return reader == null ? null : attributeValues.get(reader);
    }

    // one attribute of one renderer. WHICH KEY finds it is the reader's own answer: the server reads a column attribute
    // over the column axis, so it arrives once per column even for a property drawn per row, while the rest arrive per
    // renderer. Asking the reader is what keeps this half in step with the projection (GReactFormData splits a list
    // property's column and cell entries by the same isColumnAttribute), so an lsf property - drawn HERE and
    // captioned THERE - cannot end up reading one attribute at two different keys.
    private PValue readAttribute(GPropertyReader reader, GGroupObjectValue rendererKey) {
        NativeHashMap<GGroupObjectValue, PValue> values = attribute(reader);
        if (values == null)
            return null;
        return values.get(reader.isColumnAttribute(property) ? panelController.getColumnKey(rendererKey) : rendererKey);
    }

    private void updateRenderer(GGroupObjectValue rendererKey, PanelRenderer renderer) {
        PValue valueElementClass = readAttribute(property.valueElementClassReader, rendererKey);
        PValue font = readAttribute(property.fontReader, rendererKey);
        PValue background = readAttribute(property.backgroundReader, rendererKey);
        PValue foreground = readAttribute(property.foregroundReader, rendererKey);
        PValue placeholder = readAttribute(property.placeholderReader, rendererKey);
        PValue pattern = readAttribute(property.patternReader, rendererKey);
        PValue regexp = readAttribute(property.regexpReader, rendererKey);
        PValue regexpMessage = readAttribute(property.regexpMessageReader, rendererKey);
        PValue valueTooltip = readAttribute(property.valueTooltipReader, rendererKey);
        PValue propertyCustomOption = readAttribute(property.propertyCustomOptionsReader, rendererKey);
        PValue defaultValue = readAttribute(property.defaultValueReader, rendererKey);

        renderer.update(values.get(rendererKey),
                loadings != null && PValue.getBooleanValue(loadings.get(rendererKey)),
                attribute(property.imageReader) != null ? PValue.getImageValue(readAttribute(property.imageReader, rendererKey)) : null,
                valueElementClass == null ? property.valueElementClass : PValue.getClassStringValue(valueElementClass),
                font == null ? property.font : PValue.getFontValue(font),
                background == null ? property.getBackground() : PValue.getColorStringValue(background),
                foreground == null ? property.getForeground() : PValue.getColorStringValue(foreground),
                attribute(property.readOnlyReader) == null ? null : PValue.get3SBooleanValue(readAttribute(property.readOnlyReader, rendererKey)),
                placeholder == null ? property.placeholder : PValue.getStringValue(placeholder),
                pattern == null ? property.getPattern() : PValue.getStringValue(pattern),
                regexp == null ? property.regexp : PValue.getStringValue(regexp),
                regexpMessage == null ? property.regexpMessage : PValue.getStringValue(regexpMessage),
                valueTooltip == null ? property.valueTooltip : PValue.getStringValue(valueTooltip),
                defaultValue == null ? property.defaultValue : PValue.getStringValue(defaultValue),
                propertyCustomOption);

        // these are SET only when the property actually has the reader: unlike the arguments above they have no static
        // fallback here, so setting them from a missing attribute would clear what the design put there
        if (attribute(property.captionReader) != null)
            renderer.setCaption(GGridPropertyTable.getDynamicCaption(readAttribute(property.captionReader, rendererKey)));

        if (attribute(property.changeKeyReader) != null)
            renderer.setChangeKey(PValue.getBindingValue(readAttribute(property.changeKeyReader, rendererKey)));

        if (attribute(property.changeMouseReader) != null)
            renderer.setChangeMouse(PValue.getBindingValue(readAttribute(property.changeMouseReader, rendererKey)));

        if (attribute(property.captionElementClassReader) != null)
            renderer.setCaptionElementClass(PValue.getClassStringValue(readAttribute(property.captionElementClassReader, rendererKey)));

        if (attribute(property.commentReader) != null)
            renderer.setComment(GGridPropertyTable.getDynamicComment(readAttribute(property.commentReader, rendererKey)));
        if (attribute(property.commentElementClassReader) != null)
            renderer.setCommentElementClass(PValue.getClassStringValue(readAttribute(property.commentElementClassReader, rendererKey)));

        if (attribute(property.tooltipReader) != null)
            renderer.setTooltip(GGridPropertyTable.getDynamicTooltip(readAttribute(property.tooltipReader, rendererKey)));
    }

    public boolean focus(FocusUtils.Reason reason) {
        if (renderers == null || renderers.isEmpty()) {
            return false;
        }

        PanelRenderer toFocus;
        if (rendererKeys == null)
            toFocus = renderers.firstValue();
        else {
            GGroupObjectValue focusKey = panelController.getFocusKey(rendererKeys); // the first column, or the CURRENT row
            toFocus = focusKey != null ? renderers.get(focusKey) : null;
        }
        return toFocus != null && toFocus.focus(reason);
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



    // ONE entry point for every attribute: the reader says what it is, so nothing here has to name them one by one
    public void setAttributeValues(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        attributeValues.put(reader, values);
    }

    public void setShowIfs(NativeHashMap<GGroupObjectValue, PValue> showIfs) {
        if (!GwtSharedUtils.nullEquals(this.showIfs, showIfs)) {
            this.showIfs = showIfs;

            rendererKeysUpdated = !panelController.isSingleRenderer(property);
        }
    }

    // the keys the property is drawn for: the panel controller turns the server's column keys into them
    public void updateKeys(ArrayList<GGroupObjectValue> columnKeys) {
        setRendererKeys(panelController.getRendererKeys(columnKeys));
    }

    // returns whether these are different keys from the ones the renderers stand for, so a caller that reconciles on
    // key changes can tell them from the far more common update that only carried values
    public boolean setRendererKeys(ArrayList<GGroupObjectValue> rendererKeys) {
        if (GwtSharedUtils.nullEquals(this.rendererKeys, rendererKeys))
            return false;

        this.rendererKeys = rendererKeys;

        rendererKeysUpdated = !panelController.isSingleRenderer(property);
        return true;
    }







    // the renderer a full key is about, or null. The panel says which part of the key identifies it - the ROW when
    // the property is drawn once per row, the COLUMN otherwise - and getting that wrong is silent: the lookup simply
    // misses, so the caller records no optimistic value, no loading state and no request to reconcile, and a lost edit
    // reads as a slow server rather than as a bug.
    private PanelRenderer getRendererForFullKey(GGroupObjectValue fullCurrentKey) {
        GGroupObjectValue rendererKey = panelController.getRendererKey(property, fullCurrentKey);
        return rendererKey != null ? renderers.get(rendererKey) : null;
    }

    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GGroupObjectValue fullCurrentKey, PValue value) {
        PanelRenderer panelRenderer = getRendererForFullKey(fullCurrentKey);
        if(panelRenderer == null)
            return null;
        // the key the form records the pending change under: the renderer's own, so nothing re-derives it here
        return new Pair<>(panelRenderer.getRendererKey(), panelRenderer.setLoadingValue(value));
    }

    @Override
    public void startEditing(GGroupObjectValue fullCurrentKey) {
        PanelRenderer panelRenderer = getRendererForFullKey(fullCurrentKey);
        if(panelRenderer == null)
            return;
        panelRenderer.startEditing();
    }

    @Override
    public void stopEditing(GGroupObjectValue fullCurrentKey) {
        PanelRenderer panelRenderer = getRendererForFullKey(fullCurrentKey);
        if(panelRenderer == null)
            return;
        panelRenderer.stopEditing();
    }












}
