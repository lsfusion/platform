package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

import static java.lang.Boolean.TRUE;

// a set of property draws shown as panel renderers. What differs between the kinds of panel is not HOW the readers
// are forwarded - that is all here - but what a renderer's key means and where its widget goes, which each subclass
// answers with the placement methods below.
public abstract class GAbstractPanelController extends GPropertyController {

    protected final NativeSIDMap<GPropertyDraw, GPropertyPanelController> propertyControllers = new NativeSIDMap<>();

    protected GAbstractPanelController(GFormController formController) {
        super(formController);
    }

    @Override
    public void updateCellGridElementClasses(GGridElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    // every ATTRIBUTE reader lands here: which property it belongs to and which axis it was read over are the reader's
    // own answers, so this does not repeat them per attribute. The value itself, its loading flag and showIf are NOT
    // attributes and keep their own paths (updateProperty / updateLoadings / updateShowIfValues).
    private void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setAttributeValues(reader, values);

        updatedProperties.put(property, TRUE);
    }

    // ---- how this controller's renderers are keyed and placed ----
    //
    // A property draw has one panel renderer per key, and everything below depends on what a key MEANS for the
    // controller drawing it: which part of a full key identifies a renderer, whether it draws a row, where its widget
    // goes, whether the key order is a layout order, which renderers should exist at all, and which one focus goes to.
    //
    // The FORM's panel keys them by COLUMN - one renderer per column group value, all of them in the property's own
    // panel, in key order. A group's ROWS key them by row - the property is LSF on a grid that a CUSTOM REACT
    // view draws, so there is one renderer per row and React decides where each of them appears.

    // which part of a full key identifies the renderer. Getting this wrong is silent: the lookup simply misses, so the
    // caller records no optimistic value, no loading state and no request to reconcile, and a lost edit reads as a slow
    // server rather than as a bug
    public abstract GGroupObjectValue getRendererKey(GPropertyDraw property, GGroupObjectValue fullCurrentKey);

    // where the readers the server reads over the COLUMN axis land for this renderer - they arrive at the column key
    // even for a grid property, while the rest arrive per cell (GPropertyReader.isColumnAttribute says which is which)
    public abstract GGroupObjectValue getColumnKey(GGroupObjectValue rendererKey);

    // the ROW a renderer draws, or null when it draws its group's current object. It becomes the renderer's row key,
    // which is what makes its full key - and so every value it reads and every change it makes - address that row
    public abstract GGroupObjectValue getRowKey(GGroupObjectValue rendererKey);

    public abstract void addRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view, int index);

    public abstract void removeRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view);

    // whether a key that moved to another position has to be rebuilt there. In a panel the position IS the layout, so
    // it does; for rows the order is merely the one they arrived in, and rebuilding on it would drop focus and any edit
    // in progress on every re-sort
    public abstract boolean recreateRendererOnMove(int oldIndex, int newIndex);

    // whether these renderers should exist at all right now. A hidden panel property makes one only to carry a key or
    // mouse binding; a per-row property is the row's actual editor, so it always does
    public abstract boolean shouldCreateRenderers(GPropertyDraw property);

    // the widget the FORM lays out for this property, or null when the renderer made at init is that widget itself
    // (a plain panel property) or when there is nothing for the form to lay out (a per-row property)
    public abstract Widget getFormWidget(GPropertyDraw property);

    // the keys this property should have renderers for, given the column keys the server sent. They ARE those keys
    // unless the property is drawn per row, in which case it is drawn for the group's rows and the column axis is empty
    public abstract ArrayList<GGroupObjectValue> getRendererKeys(ArrayList<GGroupObjectValue> columnKeys);

    // whether this property has exactly ONE renderer, known in advance and made by initView. Otherwise they come and go
    // with the keys: one per column value when grouped in columns, one per row when drawn per row, and none at all for
    // a hidden property with no binding to carry
    public abstract boolean isSingleRenderer(GPropertyDraw property);

    // whether these renderers take part in the form's key and mouse bindings. A binding is form-global, so it cannot
    // mean "the third row"; and CHANGEKEY / CHANGEMOUSE are read on the column axis, so every row would carry the same
    // one anyway
    public abstract boolean shouldRegisterBindings();

    // the key that focus goes to, or null when there is nothing to focus
    public abstract GGroupObjectValue getFocusKey(ArrayList<GGroupObjectValue> keys);

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setLoadings(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if(!updateKeys) {
            if (propertyController == null) {
                propertyController = new GPropertyPanelController(property, formController, this);

                // null when the form lays nothing out for this property: it is drawn once per row, inside the rows
                ComponentWidget view = propertyController.initView();
                if (view != null)
                    getFormLayout().addBaseComponent(property, view, (FocusUtils.Reason reason) -> focusFirstWidget(reason));

                propertyControllers.put(property, propertyController);
            }
            propertyController.updateKeys(columnKeys);
        }
        propertyController.setPropertyValues(values, updateKeys);

        updatedProperties.put(property, Boolean.TRUE);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        GPropertyPanelController propertyController = propertyControllers.remove(property);

        if (propertyController != null && propertyController.hasFormView())
            getFormLayout().removeBaseComponent(property);
    }

    private NativeSIDMap<GPropertyDraw, Boolean> updatedProperties = new NativeSIDMap<>();

    public void update() {
        updatedProperties.foreachKey(property -> propertyControllers.get(property).update());
        updatedProperties.clear();
    }

    // the form is closing: nothing will ask these renderers for anything again, and a renderer left behind stays
    // reachable from MainFrame's static color-theme listener list for the life of the page - EVERY panel renderer,
    // the form panel's and the per-row ones alike
    public void destroy() {
        propertyControllers.foreachValue(GPropertyPanelController::destroyRenderers);
    }

    public boolean isEmpty() {
        return propertyControllers.isEmpty();
    }

    public boolean containsProperty(GPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }

    @Override
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateCellCaptionElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateCellFooterElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setShowIfs(values);

        updatedProperties.put(property, TRUE); // in grid it is a little bit different (when removing showif, updatedProperties is not flagged), however it's not that important
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updatePropertyComments(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updatePropertyCustomOptionsValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateChangeKeyValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateChangeMouseValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateDefaultValueValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        updateAttribute(reader, values);
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        GPropertyPanelController propertyPanelController = propertyControllers.get(propertyDraw);
        if (propertyPanelController != null) {
            propertyPanelController.focus(FocusUtils.Reason.ACTIVATE);
        }
    }

    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        if (propertyControllers.isEmpty()) {
            return false;
        }

        for (GPropertyDraw property : formController.getPropertyDraws()) {
            GPropertyPanelController propController = propertyControllers.get(property);
            if (propController != null) {
                if (property.isFocusable() && propController.focus(reason)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        GPropertyPanelController panelController = propertyControllers.get(property);
        return panelController != null ? panelController.setLoadingValueAt(fullCurrentKey, value) : null;
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return containsProperty(property);
    }
}
