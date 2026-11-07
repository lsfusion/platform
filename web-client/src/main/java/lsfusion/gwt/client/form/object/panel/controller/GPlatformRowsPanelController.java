package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.design.view.GReactFormData;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.ArrayList;

// a panel controller whose keys are ROWS: it draws a group's platform-drawn GRID properties once per row of that group
// - the form's GPlatformFormPanelController draws the PANEL ones -, and the group's CUSTOM REACT view places each
// renderer into the element it rendered for that row.
//
// Not to be confused with the GRID's `record` container, which is an ordinary panel container whose content follows the
// group's CURRENT object (the map and calendar popups). Nothing here is cloned from a design container.
//
// It is a panel controller because such a renderer IS a panel renderer - same widget, same readers, same edit path - so
// the reader forwarding comes from the shared base unchanged; what differs is only how a key is read and where the
// widget goes, which is what the base leaves to each kind of panel.
public final class GPlatformRowsPanelController extends GPlatformPanelController {

    private final GGroupObject group;
    // the rows its renderers follow: the rows' part where React draws them - a platform-drawn list property is only
    // where React draws its group's rows (FormView.isPlatformDrawn)
    private final GReactFormData.Rows rows;

    // the element React gave for each (row, property), and where that property's renderer for that row is put. The
    // renderer itself is NOT kept here - the property's own controller is the record of it - and the two arrive in
    // either order: React can render a row before its renderer exists, and a renderer can exist before React renders
    // anything. The mirror of the form panel's columnPanels: keyed by the axis this controller draws on.
    private final PlacedViews<RowProperty> placed = new PlacedViews<>(new PlacedViews.Views<RowProperty>() {
        @Override
        public PlacedViews.Problem problem(RowProperty key) {
            // the group's rows decide, and not whether a renderer happens to still exist: React renders before the
            // renderers are reconciled, so a row already gone can still have the one that is about to be destroyed
            if (!getRendererKeys().contains(key.row))
                return new PlacedViews.Problem("the row given to '" + key.property.sID + "' is not in the group any more",
                        "the row given to '" + key.property.sID + "' is not one of the group's rows any"
                                + " more, so nothing will ever be placed there");
            return null;
        }

        @Override
        public boolean place(RowProperty key, Element host) {
            ComponentViewWidget view = getView(key.row, key.property);
            if (view == null)
                return false;

            view.appendTo(host);
            GwtClientUtils.clearCustomViewError(host);
            return true;
        }

        @Override
        public void park(RowProperty key) {
            ComponentViewWidget view = getView(key.row, key.property);
            if (view != null) // back to waiting, NOT destroyed: React unmounts a row merely by scrolling it out of view
                view.appendTo(getParkElement());
        }

        // the showing widget, which every kind of renderer view has - an inline one (a caption beside the value) has no
        // single widget
        @Override
        public void resized(RowProperty key) {
            FlexTabbedPanel.scheduleOnResize(getView(key.row, key.property).getShowingWidget());
        }
    });

    // JSX can name the same property differently: the diagnostic keeps that spelling, while ownership depends only
    // on the row and the property
    private static final class RowProperty {
        final GGroupObjectValue row;
        final GPropertyDraw property;
        final String name;

        RowProperty(GGroupObjectValue row, GPropertyDraw property, String name) {
            this.row = row;
            this.property = property;
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof RowProperty && row.equals(((RowProperty) other).row)
                    && property.equals(((RowProperty) other).property);
        }

        @Override
        public int hashCode() {
            return 31 * row.hashCode() + property.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public GPlatformRowsPanelController(GFormController formController, GGroupObject group, GReactFormData.Rows rows) {
        super(formController);

        this.group = group;
        this.rows = rows;
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
        // the group's own ROW key - which for a group of a TREE is the whole path down to it. The renderers are
        // registered under getRendererKeys(), i.e. under exactly those keys, so narrowing to the group's own objects
        // here made every lookup miss - and a missed renderer records no optimistic value and no loading state, so a
        // lost edit reads as a slow server rather than as a bug
        return property.groupObject.getRowKey(fullCurrentKey);
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
        // while its element waits there until React shows the row - the same park the platform-drawn children use
        view.attachTo(formController.getFormLayout().attachContainer, getParkElement());

        placed.retryPending(new RowProperty(rendererKey, property, property.sID));
    }

    @Override
    public void removeRenderer(GGroupObjectValue rendererKey, GPropertyDraw property, ComponentViewWidget view) {
        placed.viewRemoved(new RowProperty(rendererKey, property, property.sID), true);
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
        // the server's column keys describe an axis this property does not use: it is drawn per ROW
        return getRendererKeys();
    }

    @Override
    public boolean isSingleRenderer(GPropertyDraw property) {
        return false; // one renderer per row of the group, so they come and go with the rows
    }

    @Override
    public GGroupObjectValue getFocusKey(ArrayList<GGroupObjectValue> keys) {
        // focus belongs to the CURRENT row, never to whichever row happened to be placed first - and the projection
        // already knows which that is, including right after the view changed it optimistically
        return formController.getGroupController(group).getSelectedKey();
    }

    // ---- hosts, registered by the react view as it renders rows ----

    public boolean place(String name, GGroupObjectValue rowKey, GPropertyDraw property, Element host) {
        return placed.mount(new RowProperty(rowKey, property, name), host);
    }

    public void unplace(GGroupObjectValue rowKey, GPropertyDraw property, Element host) {
        placed.unmount(new RowProperty(rowKey, property, property.sID), host);
    }

    // ---- data ----

    // the rows this group is showing, straight from the part React draws them as, which already tracks them - including
    // an optimistic add or delete, which a copy taken from the server's changes would not see until the server
    // confirmed it. The current object is read the same way, and for the same reason. The part's own list, not a copy:
    // it puts a new list in place whenever the rows change and never changes one (GReactFormData.Rows), so the
    // controllers may keep the one they are given to compare the next set against.
    private ArrayList<GGroupObjectValue> getRendererKeys() {
        return rows.getRows();
    }

    // the renderers follow the group's rows, not what React has shown: a renderer that existed only while its row was
    // on screen would be rebuilt whenever React re-rendered, losing focus and any edit halfway through a keystroke
    public void updateRowRenderers() {
        ArrayList<GGroupObjectValue> rows = getRendererKeys();
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

    // the widget of the renderer this property draws for this row, from the property's own controller - the single
    // record of it, so a renderer that controller dropped can never be found here
    private ComponentViewWidget getView(GGroupObjectValue rowKey, GPropertyDraw property) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        PanelRenderer renderer = propertyController != null ? propertyController.getRenderer(rowKey) : null;
        return renderer != null ? renderer.getComponentViewWidget() : null;
    }
}
