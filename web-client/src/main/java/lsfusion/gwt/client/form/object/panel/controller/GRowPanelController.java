package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// a panel controller whose keys are ROWS: it draws a group's LSF properties once per row of that group, and the
// group's CUSTOM REACT view places each renderer into the element it rendered for that row.
//
// Not to be confused with the GRID's `record` container, which is an ordinary panel container whose content follows the
// group's CURRENT object (the map and calendar popups). Nothing here is cloned from a design container.
//
// It is a panel controller because such a renderer IS a panel renderer - same widget, same readers, same edit path - so
// the reader forwarding comes from the shared base unchanged; what differs is only how a key is read and where the
// widget goes, which is what the base leaves to each kind of panel.
public class GRowPanelController extends GAbstractPanelController {

    private final GGroupObject group;

    // the element React gave for each (row, property), and where that property's renderer for that row is put. The
    // renderer itself is NOT kept here - the property's own controller is the record of it - and the two arrive in
    // either order: React can render a row before its renderer exists, and a renderer can exist before React renders
    // anything. The mirror of the form panel's columnPanels: keyed by the axis this controller draws on.
    private final NativeHashMap<GGroupObjectValue, Map<String, Element>> rowHosts = new NativeHashMap<>();

    public GRowPanelController(GFormController formController, GGroupObject group) {
        super(formController);

        this.group = group;
    }

    // a property can be dropped outright - its own SHOWIF going false does that - with no key diff preceding it, so its
    // renderers have to be taken out here, or they would stay in their rows, editable, after the server hid them.
    // Its HOSTS deliberately stay, unlike the form panel's columnPanels: React is still rendering those rows and will
    // not call place again for them, so dropping the hosts would leave the property unplaceable if it comes back
    @Override
    public void removeProperty(GPropertyDraw property) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if (propertyController != null)
            propertyController.removeAllRenderers();

        super.removeProperty(property);
    }


    // ---- placement ----

    @Override
    public GGroupObjectValue getRendererKey(GPropertyDraw property, GGroupObjectValue fullCurrentKey) {
        return property.groupObject.filterRowKeys(fullCurrentKey);
    }

    @Override
    public GGroupObjectValue getColumnKey(GGroupObjectValue rendererKey) {
        return GGroupObjectValue.EMPTY; // a per-row property has no column groups, so that axis is empty for every row
    }

    @Override
    public GGroupObjectValue getRowKey(GGroupObjectValue rendererKey) {
        return rendererKey; // the key IS the row: that is what makes the renderer read and write that row
    }

    @Override
    public void addRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view, int index) {
        // a logical child of the form's (invisible) attach container, so GWT attaches it and it has events and sizing,
        // while its element waits there until React shows the row - the same park the lsf children use
        view.attachTo(formController.getFormLayout().attachContainer, getParkElement());

        Element host = getHost(rendererKey, property.sID);
        if (host != null) {
            view.appendTo(host);
            GwtClientUtils.clearLsfViewError(host); // a host that was told its row is gone, and whose row came back
        }
    }

    @Override
    public void removeRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view) {
        view.remove(formController.getFormLayout().attachContainer);
    }

    @Override
    public boolean recreateRendererOnMove(int oldIndex, int newIndex) {
        return false; // React decides where a row appears; the order the keys arrived in is not a layout
    }

    @Override
    public boolean shouldCreateRenderers(GPropertyDraw property) {
        return true; // a per-row renderer IS the row's editor, so it exists whenever its row does
    }

    @Override
    public Widget getFormWidget(GPropertyDraw property) {
        return null; // the form lays out nothing for this property - every renderer of it lives in a row
    }

    @Override
    public ArrayList<GGroupObjectValue> getRendererKeys(ArrayList<GGroupObjectValue> columnKeys) {
        return getRows(); // the server's column keys describe an axis this property does not use: it is drawn per ROW
    }

    @Override
    public boolean isSingleRenderer(GPropertyDraw property) {
        return false; // one renderer per row of the group, so they come and go with the rows
    }

    @Override
    public boolean shouldRegisterBindings() {
        return false; // one renderer per row, and a form-global binding cannot name a row
    }

    @Override
    public GGroupObjectValue getFocusKey(ArrayList<GGroupObjectValue> keys) {
        // focus belongs to the CURRENT row, never to whichever row happened to be placed first - and the projection
        // already knows which that is, including right after the view changed it optimistically
        return formController.getReactCurrentObject(group);
    }

    // ---- hosts, registered by the react view as it renders rows ----

    // returns the host that already had this renderer, if any: a second live host for one property AND one row is an
    // author error (two <Lsf> naming the same pair), never a legitimate move, so the FIRST one keeps it - and the
    // second is remembered, since a component that later stops rendering the first would otherwise leave the editor
    // waiting with a place for it still on the screen
    public Element place(GGroupObjectValue rowKey, GPropertyDraw property, Element host) {
        Map<String, Element> propertyHosts = getPropertyHosts(rowKey);
        Element current = propertyHosts.get(property.sID);
        if (current != null && current != host) {
            refused(rowKey, property).add(host);
            return current;
        }

        // the group's rows decide, and not whether a renderer happens to still exist: React renders before the
        // renderers are reconciled, so a row already gone can still have the one that is about to be destroyed
        if (!getRows().contains(rowKey)) {
            GwtClientUtils.showLsfViewError(host, "the row given to '" + property.sID + "' is not in the group any more");
            GwtClientUtils.logLsfViewError("the row given to '" + property.sID + "' is not one of the group's rows any"
                    + " more, so nothing will ever be placed there");
            return null;
        }

        propertyHosts.put(property.sID, host);

        ComponentViewWidget view = getView(rowKey, property);
        if (view != null) { // otherwise the host waits: the renderer arrives with the next update
            view.appendTo(host);
            GwtClientUtils.clearLsfViewError(host);
        }
        return null;
    }

    public void unplace(GGroupObjectValue rowKey, GPropertyDraw property, Element host) {
        Map<String, Element> propertyHosts = rowHosts.get(rowKey);
        if (propertyHosts == null || propertyHosts.get(property.sID) != host) { // a stale unplace, from a host that
            forget(rowKey, property, host);   // lost the row - or one that was refused and is now gone, and a host the
            return;                           // component has taken away is not one waiting for anything
        }

        propertyHosts.remove(property.sID);
        if (propertyHosts.isEmpty())
            rowHosts.remove(rowKey);

        ComponentViewWidget view = getView(rowKey, property);
        if (view != null) // back to waiting, NOT destroyed: React unmounts a row merely by scrolling it out of view
            view.appendTo(getParkElement());

        takeOver(rowKey, property); // ...and only then to a place that asked for it while this one held it
    }

    // the hosts that asked for a renderer another host already had, in the order they asked. React renders the same
    // <Lsf> until its own props change, so a host refused here is never offered again on its own
    private final NativeHashMap<GGroupObjectValue, Map<String, List<Element>>> refusedHosts = new NativeHashMap<>();

    private List<Element> refused(GGroupObjectValue rowKey, GPropertyDraw property) {
        Map<String, List<Element>> byProperty = refusedHosts.get(rowKey);
        if (byProperty == null) {
            byProperty = new HashMap<>();
            refusedHosts.put(rowKey, byProperty);
        }
        return byProperty.computeIfAbsent(property.sID, sid -> new ArrayList<>());
    }

    private void forget(GGroupObjectValue rowKey, GPropertyDraw property, Element host) {
        Map<String, List<Element>> byProperty = refusedHosts.get(rowKey);
        List<Element> asked = byProperty != null ? byProperty.get(property.sID) : null;
        if (asked != null && asked.remove(host) && asked.isEmpty()) {
            byProperty.remove(property.sID);
            if (byProperty.isEmpty())
                refusedHosts.remove(rowKey);
        }
    }

    // the host that had the renderer is gone: the eldest one that asked meanwhile takes it, if it is still in the
    // page - a component that removed both in one render leaves nothing to take over
    private void takeOver(GGroupObjectValue rowKey, GPropertyDraw property) {
        Map<String, List<Element>> byProperty = refusedHosts.get(rowKey);
        List<Element> asked = byProperty != null ? byProperty.get(property.sID) : null;
        if (asked == null)
            return;

        while (!asked.isEmpty()) {
            Element next = asked.remove(0);
            if (GwtClientUtils.isInDocument(next)) {
                GwtClientUtils.clearLsfViewError(next); // it has been holding "already placed by another <Lsf>"
                place(rowKey, property, next);
                break;
            }
        }
        if (asked.isEmpty()) {
            byProperty.remove(property.sID);
            if (byProperty.isEmpty())
                refusedHosts.remove(rowKey);
        }
    }

    // ---- data ----

    // the rows this group is showing, straight from the projection that already tracks them - including an optimistic
    // add or delete, which a copy taken from the server's changes would not see until the server confirmed it. The
    // current object is read the same way, and for the same reason.
    // a copy, because the projection mutates its own list in place when a row is added or deleted optimistically, and
    // the controllers keep the one they are given to compare the next set against. One copy is shared by all of them:
    // nothing mutates it afterwards.
    private ArrayList<GGroupObjectValue> getRows() {
        ArrayList<GGroupObjectValue> rows = formController.getReactRows(group);
        return rows != null ? new ArrayList<>(rows) : new ArrayList<>();
    }

    // the renderers follow the group's rows, not what React has shown: a renderer that existed only while its row was
    // on screen would be rebuilt whenever React re-rendered, losing focus and any edit halfway through a keystroke
    public void updateRowRenderers() {
        ArrayList<GGroupObjectValue> rows = getRows();
        propertyControllers.foreachValue(propertyController -> {
            // only when the ROWS changed: an update that carried values reaches the renderers through the ordinary
            // per-property path, and reconciling every renderer of every row for it would be most of the work of a
            // form update, done for nothing
            if (propertyController.setRendererKeys(rows))
                propertyController.update();
        });
    }

    // ---- internals ----

    private Element getParkElement() {
        return formController.getFormLayout().attachContainer.getElement();
    }

    private Map<String, Element> getPropertyHosts(GGroupObjectValue rowKey) {
        Map<String, Element> propertyHosts = rowHosts.get(rowKey);
        if (propertyHosts == null) {
            propertyHosts = new HashMap<>();
            rowHosts.put(rowKey, propertyHosts);
        }
        return propertyHosts;
    }

    private Element getHost(GGroupObjectValue rowKey, String propertySID) {
        Map<String, Element> propertyHosts = rowHosts.get(rowKey);
        return propertyHosts != null ? propertyHosts.get(propertySID) : null;
    }

    // the widget of the renderer this property draws for this row, from the property's own controller - the single
    // record of it, so a renderer that controller dropped can never be found here
    private ComponentViewWidget getView(GGroupObjectValue rowKey, GPropertyDraw property) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        PanelRenderer renderer = propertyController != null ? propertyController.getRenderer(rowKey) : null;
        return renderer != null ? renderer.getComponentViewWidget() : null;
    }
}
