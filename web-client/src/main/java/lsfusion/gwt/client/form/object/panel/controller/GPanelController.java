package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;

import static lsfusion.gwt.client.view.MainFrame.v5;

// the form's own panel: a key is a COLUMN of the property, and its renderers live in a panel of that property's own,
// in key order. A hidden property keeps its renderers - they may still carry key or mouse bindings - but places none.
public class GPanelController extends GAbstractPanelController {

    // the panel each property's renderers go into, and what the form lays out for it. Only properties that HAVE such a
    // panel are here: a property with exactly one renderer is laid out as that renderer, with nothing around it
    private final NativeSIDMap<GPropertyDraw, SizedFlexPanel> columnPanels = new NativeSIDMap<>();

    public GPanelController(GFormController formController) {
        super(formController);

        formController.addEnterBindings(GBindingMode.ALL, this::focusNextElement, null);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        super.removeProperty(property); // the form view goes first: until it does, the renderers still have a panel
        columnPanels.remove(property);
    }

    private void focusNextElement(boolean forward, NativeEvent event) {
        formController.focusNextElement(FocusUtils.Reason.KEYNEXTNAVIGATE, forward);
    }

    // ---- placement ----

    @Override
    public GGroupObjectValue getRendererKey(GPropertyDraw property, GGroupObjectValue fullCurrentKey) {
        return property.filterColumnKeys(fullCurrentKey);
    }

    @Override
    public GGroupObjectValue getColumnKey(GGroupObjectValue rendererKey) {
        return rendererKey; // the renderer key IS the column key here, so there is nothing to project
    }

    @Override
    public GGroupObjectValue getRowKey(GGroupObjectValue rendererKey) {
        return null; // a panel renderer draws its group's current object, so it has no row of its own
    }

    @Override
    public void addRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view, int index) {
        if (!property.hide) // a hidden property keeps its renderer - it may carry a binding - but shows it nowhere
            view.add(columnPanels.get(property), index); // something like getChildPosition should be done here, but for now there is a hasColumnGroupObjects check
    }

    @Override
    public void removeRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view) {
        if (!property.hide)
            view.remove(columnPanels.get(property));
    }

    @Override
    public boolean recreateRendererOnMove(int oldIndex, int newIndex) {
        return oldIndex != newIndex; // the position among the keys IS where the renderer sits in the panel
    }

    @Override
    public boolean shouldCreateRenderers(GPropertyDraw property) {
        // a hidden property still makes its renderers if they have a binding to carry - the binding works while hidden
        return !property.hide || property.hasKeyBinding() || property.hasMouseBinding();
    }

    // asked once, at init, and only for a property that has more than the one renderer - so the panel is made here,
    // and a hidden property gets one too: it is what the form lays out, it just stays empty
    @Override
    public Widget getFormWidget(GPropertyDraw property) {
        SizedFlexPanel columnPanel = new SizedFlexPanel(property.panelColumnVertical);
        GwtClientUtils.addClassName(columnPanel, "property-container-panel", "propertyContainerPanel", v5);
        columnPanels.put(property, columnPanel);
        return columnPanel;
    }

    @Override
    public ArrayList<GGroupObjectValue> getRendererKeys(ArrayList<GGroupObjectValue> columnKeys) {
        return columnKeys;
    }

    @Override
    public boolean isSingleRenderer(GPropertyDraw property) {
        // a property grouped in columns gains and loses renderers with its column keys, and a hidden one gets one only
        // if it has a binding to carry; anything else has exactly one, made once by initView and never diffed
        return !property.hasColumnGroupObjects() && !property.hide;
    }

    @Override
    public boolean shouldRegisterBindings() {
        return true;
    }

    @Override
    public GGroupObjectValue getFocusKey(ArrayList<GGroupObjectValue> keys) {
        return keys != null && !keys.isEmpty() ? keys.get(0) : null;
    }
}
