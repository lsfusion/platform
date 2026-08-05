package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashSet;
import java.util.Set;

// CUSTOM REACT 'fn': hosts a React component (window[fn], fn = container's custom) that OWNS this container's subtree,
// except for the children marked `lsf = TRUE` in DESIGN — those keep their real GWT view, are excluded from the
// projection, and the component places them with <Lsf name/>, reading their caption/image from their entry in props.data.
// The component receives props { data, controller }: `data` is the @lsfusion/core-shaped projected form state
// (re-rendered on each form change) and carries every projected thing directly, keyed as it is keyed; `controller`
// mutates the form. That is the primary, props-down contract — the
// normal optimization is React.memo or React Compiler over props.data (structural sharing keeps unchanged refs stable).
// For OPT-IN fine-grained re-render without prop-threading, descendants use the window.lsfusion hooks
// (useData(selector) over the data / useController) backed by a React context the platform installs around
// the component (react-redux's Provider + useSelector/useDispatch shape) — e.g. useData(s => s.i.list[k]).
// The hooks are zero-overhead when no component subscribes: the snapshot IS the (structurally shared) data object.
public class ReactContainerView extends ParkedContainerView {

    private final ReactRoot root;

    // sID -> the host claimed for it (the first one wins). The child's view is mounted there once it exists, so
    // the host outlives a SHOWIF drop/rebuild of the view: whether the view exists now is indexOfLsfView(sid) >= 0
    private final PlacedViews placed = new PlacedViews(new PlacedViews.Views() {
        @Override
        public PlacedViews.Problem problem(String sid) {
            GComponent declared = findDeclared(sid);
            if (declared == null)
                return new PlacedViews.Problem("'" + sid + "' is not a child of '" + container.sID + "'",
                        "component '" + sid + "' is not a child of container '" + container.sID + "'");
            if (!declared.isLsfView()) // a child of the container, but without lsf = TRUE, so React owns it
                return new PlacedViews.Problem("'" + sid + "' has no lsf = TRUE",
                        "child '" + sid + "' of container '" + container.sID + "' has no `lsf = TRUE`: React owns it, so there is no GWT view to place");
            return null;
        }

        @Override
        public boolean place(String sid, Element host) {
            int index = indexOfLsfView(sid);
            if (index < 0) // the view does not exist yet - a SHOWIF dropped it, or it is not built - so the host waits
                return false;

            attachView(index, host);
            return true;
        }

        @Override
        public void park(String sid) {
            int index = indexOfLsfView(sid);
            if (index >= 0) // if a SHOWIF already dropped the view there is nothing to park
                parkChild(index);
        }
    });
    private final Set<String> reactHidden = new HashSet<>(); // sIDs the server has been told the component is not showing
    private boolean unmountingRoot; // true while the React root is torn down (form close), so no per-child hide is sent

    public ReactContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        // row is absent for an lsf CHILD - one view, one host - and is a row of the group for an LSF grid property,
        // which has one renderer, and so one host, per row
        root = new ReactRoot(container.getCustom(), formController.controller, new ReactRoot.Placement() {
            @Override
            public void mount(String name, Element host, JavaScriptObject row) {
                mountComponent(name, host, row);
            }

            @Override
            public void unmount(String name, Element host, JavaScriptObject row) {
                unmountComponent(name, host, row);
            }
        });
        GwtClientUtils.addClassName(panel, "panel-react");
        panel.addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(panel.getElement());
            else
                unmount();
        });
    }

    @Override
    protected void addImpl(int index) {
        // React owns the subtree, so only an lsf child gets a GWT view here
        super.addImpl(index);

        // the view is built by its controller, and a property hidden with `remove` is dropped and built again as it
        // comes back, long after React rendered the host. A host never re-runs its ref for that, so the host that has
        // been waiting is filled here
        placed.retryPending();
    }

    @Override
    protected void removeImpl(int index) {
        // a SHOWIF took this child's view away. The host stays remembered - it will be filled again when the view comes
        // back - but the placement must not: otherwise nothing would put the rebuilt view into it
        placed.viewRemoved(children.get(index).sID, true);

        super.removeImpl(index);
    }

    // called back by the host's ref, and the cleanup is its exact inverse (F1). React runs every ref detach of a commit
    // before any attach, so a mount can never race an unmount of the same child: a second live host for one sid is
    // always a duplicate host, never a legitimate move.
    private void mountComponent(String sid, Element host, JavaScriptObject row) {
        stampHost(host, sid); // marks the host even when the mount reports a problem below

        if (row != null) { // an LSF grid property: one renderer per ROW, so the row says WHICH of them goes here
            GPropertyDraw property = getRowLsfViewProperty(sid);
            GGroupObjectValue rowKey = GGroupObjectValue.resolveObject(row);
            if (property == null || rowKey == null) {
                reportUnknownRow(sid, host, property == null);
                return;
            }
            // the property, not the sid JSX used: the ledger of who holds which row is the property's own
            if (formController.getRowPanelController(property).place(rowKey, property, host) != null)
                PlacedViews.reportDuplicate(sid, host); // another <Lsf> already placed this property for this row
            return;
        }

        // a property drawn per ROW has no single view to place: which of its renderers goes here is the row's to say.
        // Without the row, the one-view bookkeeping below waits for a view that is never built - silently, for good -
        // and a name spelled without PROPERTY(...) does not even read as a child, so the reason given was the wrong one
        GPropertyDraw perRow = getRowLsfViewProperty(sid);
        if (perRow != null) {
            GwtClientUtils.showLsfViewError(host, "'" + sid + "' is drawn per row, so its <Lsf> needs a row");
            GwtClientUtils.logLsfViewError("'" + sid + "' is an LSF property drawn per row: its <Lsf> is given the row it"
                    + " belongs to, as <Lsf name row/>, since each row has a renderer of its own");
            return;
        }

        if (placed.mount(sid, host))
            setReactHidden(sid, false); // React shows this child now, so the server may read its data again
    }

    // tell the server whether the React component is showing this lsf child, so a hidden child's data is not read
    // (like collapse / tab activation). Only a real change is sent; an lsf child is shown by default on the server
    private void setReactHidden(String sid, boolean hidden) {
        if (hidden ? reactHidden.add(sid) : reactHidden.remove(sid))
            formController.setUserHidden(findDeclared(sid), hidden);
    }

    private void attachView(int index, Element host) {
        ComponentViewWidget childView = getChildView(index);
        childView.appendTo(host);
        // an lsf child is one self-contained view (a property is forced non-inline in PropertyPanelRenderer, so it is
        // a single widget, not inline value/comment siblings) — getSingleWidget is that view. Stretch it to fill the host
        // with the platform fill-parent-flex(-cont) classes — the same fill a native SizedFlexPanel child gets; the
        // min-size reset those classes omit is added in layout.css.
        GwtClientUtils.setupFlexParent(childView.getSingleWidget().widget.getElement());
        resizeChildren(); // the child moved out of the display:none park into a laid-out place
    }

    // the host may be an element the component renders for its own layout (useLsf), so the platform marks it
    // rather than expecting the marks
    private void stampHost(Element host, String sid) {
        GwtClientUtils.addClassName(host, "lsf-view");
        host.setAttribute("data-lsf-sid", sid);
    }

    private void clearHost(Element host) {
        GwtClientUtils.removeClassName(host, "lsf-view");
        host.removeAttribute("data-lsf-sid");
    }

    private void unmountComponent(String sid, Element host, JavaScriptObject row) {
        if (row != null) { // the renderer goes back to waiting, it is NOT dropped: React unmounts a row by scrolling it out
            GPropertyDraw property = getRowLsfViewProperty(sid);
            GGroupObjectValue rowKey = GGroupObjectValue.resolveObject(row);
            if (property != null && rowKey != null)
                formController.getRowPanelController(property).unplace(rowKey, property, host);
            clearHost(host);
            return;
        }

        if (placed.unmount(sid, host) && !unmountingRoot) // a real hide by React, not the form closing, which resets
            setReactHidden(sid, true);                   // the server's hidden set anyway

        clearHost(host); // the host outlives the mount: it may be the component's own element, or React may reuse it
    }

    private int indexOfLsfView(String sid) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);
            if (child.isLsfView() && child.sID.equals(sid))
                return i;
        }
        return -1;
    }

    // an LSF grid property declared in this container, or null. Unlike an lsf CONTAINER child, it has one
    // renderer per row rather than one view, so it is placed through its row panel controller instead of `hosts`
    private GPropertyDraw getRowLsfViewProperty(String sid) {
        GComponent declared = findDeclared(sid); // by the design identifier, PROPERTY(qty) - the one spelling there is
        return declared instanceof GPropertyDraw && ((GPropertyDraw) declared).isLsfViewPerRow() ? (GPropertyDraw) declared : null;
    }

    // a per-row mount that cannot be resolved: either the sid names no LSF grid property of this container, or
    // what was passed as the row is not one. Both would leave a silently empty cell, so both are shown in the page
    private void reportUnknownRow(String sid, Element host, boolean unknownSid) {
        if (unknownSid) {
            GwtClientUtils.showLsfViewError(host, "'" + sid + "' is not an LSF grid property");
            GwtClientUtils.logLsfViewError("component '" + sid + "' of container '" + container.sID + "' is not a grid property marked LSF, so it is not drawn per row");
        } else {
            GwtClientUtils.showLsfViewError(host, "the `row` given to '" + sid + "' is not a row");
            GwtClientUtils.logLsfViewError("the `row` passed to '" + sid + "' does not identify a row: pass the row object from the projected data, not its key");
        }
    }

    private GComponent findDeclared(String sid) {
        for (GComponent child : container.children)
            if (sid.equals(child.sID))
                return child;
        return null;
    }

    public GContainer getContainer() {
        return container;
    }

    // pushed from GFormController.applyRemoteChanges after each form change. An lsf child's caption / captionClass /
    // image now lives in its own entry in `data` (built by GReactFormData): an attribute change marks the scope dirty,
    // so build() returns a new top ref and the data change alone re-renders React — no separate components channel.
    public void updateData(JavaScriptObject data) {
        root.updateData(data);
    }



    private void unmount() {
        unmountingRoot = true;
        root.unmount(); // runs the hosts' ref cleanups, which park every mounted child and drop it from `hosts`
        unmountingRoot = false;
        placed.reset(); // any host left waiting (its view dropped by SHOWIF) belongs to the old tree; drop it too
        reactHidden.clear(); // the server's FormInstance is reset on the next open, so its hidden set starts empty too
    }

}
