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

import java.util.*;

// CUSTOM REACT 'fn': a React component (window[fn], fn = the container's custom) draws this container's subtree
// from the form's projection - its props { data, controller } (GReactFormData), which the window.lsfusion hooks read
// too - except the `lsf = TRUE` components it places: those keep their GWT view, and the component places each with
// <Lsf name/>, an LSF grid property with <Lsf name row/>, one renderer per row
public class ReactContainerView extends ParkedContainerView {

    private final ReactRoot root;
    // this container's state in the form's projection: what its `props.data` is built from, and its `props.controller`
    private final GReactFormData.ContainerState state;

    // the components this view places, by sID: those it is the React place of (GComponent.getReactPlace, the rule the
    // layout and the projection ask too) - its children and what is inside the containers it draws itself. What an
    // <Lsf> name means is looked up here
    private final Map<String, GComponent> placeable = new HashMap<>();
    // ... and of them the lsf ones, placed whole, one view each, named or not: what the server is told of
    // (reconcilePlaced)
    private final List<GComponent> lsfViews = new ArrayList<>();

    // sID -> the host claimed for it (the first one wins). The child's view is mounted there once it exists, so
    // the host outlives a SHOWIF drop/rebuild of the view: whether the view exists now is indexOfLsfView(sid) >= 0
    private final PlacedViews placed = new PlacedViews(new PlacedViews.Views() {
        @Override
        public PlacedViews.Problem problem(String sid) {
            GComponent component = placeable.get(sid);
            if (component == null)
                return new PlacedViews.Problem("'" + sid + "' is not inside '" + container.sID + "'",
                        "component '" + sid + "' is not inside container '" + container.sID + "', nor in a container it draws itself");
            if (!component.isLsfView()) // inside the container, but without lsf = TRUE, so React draws it itself
                return new PlacedViews.Problem("'" + sid + "' has no lsf = TRUE",
                        "'" + sid + "' inside container '" + container.sID + "' has no `lsf = TRUE`, and only a"
                                + " component marked so is placed: mark it `lsf = TRUE` to place it with <Lsf>");
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
    // what the server has been told about the lsf children: the SET this view places now, not the events that led to it
    // (reconcilePlaced). The children it was told are hidden - a child the server was told nothing about is shown, the
    // server's default
    private final Set<GComponent> reportedHidden = new HashSet<>();
    // a pending reconcile of an older generation is dropped: the root was unmounted since
    private int reconcileGeneration;
    private boolean reconcilePending;

    public ReactContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        addPlaceable(container);
        // THIS projection's controller: it names what this container's own `props.data` carries and nothing else -
        // a view that cannot see its neighbour does not name it either. The classic surfaces (a custom group view, a
        // custom cell editor, an INTERNAL CLIENT action) keep the form's own controller
        state = formController.addReactContainer(container, this::updateData);
        root = new ReactRoot(container.getCustom(), state.controller, new ReactRoot.Placement() {
            @Override
            public void mount(String name, Element host, JavaScriptObject row) {
                getPlacement(name).mount(host, row);
            }

            @Override
            public void unmount(String name, Element host, JavaScriptObject row) {
                getPlacement(name).unmount(host, row);
            }

            @Override
            public boolean marksHosts() {
                return true; // a host here holds a view of the form, which the layout styles by its marks
            }
        });
        GwtClientUtils.addClassName(panel, "panel-react");
        panel.addAttachHandler(event -> {
            if (event.isAttached()) {
                // drawn now: a tree mounted again settles what it places too, even when that is nothing
                if (root.mount(panel.getElement(), formController.isSizeFixedOnShow()))
                    scheduleReconcile();
            } else
                unmount();
        });
    }

    // what is under the container whose React place it is: the design is fixed, so this is taken once. Not an LSF grid
    // property: it is no view of its own here, its renderers are placed per row by the view that draws its group's rows
    // (getPlacement)
    private void addPlaceable(GComponent component) {
        for (GComponent child : component.getChildren()) {
            if (child.getReactPlace() == container && !isPlacedPerRow(child)) {
                if (child.sID != null) // what an <Lsf> name can mean
                    placeable.put(child.sID, child);
                if (child.isLsfView())
                    lsfViews.add(child);
            }
            addPlaceable(child);
        }
    }
    private static boolean isPlacedPerRow(GComponent component) {
        return component instanceof GPropertyDraw && ((GPropertyDraw) component).isLsfViewPerRow();
    }

    // React puts each view where an <Lsf> names it, so the order the views are held in means nothing: a new one goes
    // last. This view holds the views of what it places at any depth, not only of its container's own children
    @Override
    protected int getInsertIndex(GComponent child) {
        return children.size();
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

    // HOW an <Lsf name> places what it names, by what the name is: an LSF grid property of a group this view draws has
    // a renderer per ROW, any other component one view. Mount and unmount are called back by the host's ref, the
    // cleanup the exact inverse (F1). React runs every ref detach of a commit before any attach, so a mount can never
    // race an unmount of the same child: a second live host for one sid is always a duplicate, never a legitimate move
    private LsfPlacement getPlacement(String sid) {
        GPropertyDraw property = state.getRowLsfViewProperty(sid);
        return property != null ? new LsfRowPlacement(sid, property) : new LsfViewPlacement(sid);
    }
    private abstract static class LsfPlacement {
        final String sid;

        LsfPlacement(String sid) {
            this.sid = sid;
        }

        abstract void mount(Element host, JavaScriptObject row);
        abstract void unmount(Element host, JavaScriptObject row);
    }
    // ... one view, mounted in the host claimed for it (`placed`); a row would say which of several renderers is meant,
    // and there is one view, so a row given is a mistake shown in the host
    private final class LsfViewPlacement extends LsfPlacement {
        LsfViewPlacement(String sid) {
            super(sid);
        }

        @Override
        void mount(Element host, JavaScriptObject row) {
            if (row != null) {
                GwtClientUtils.showLsfViewError(host, "'" + sid + "' is not an LSF grid property of this view");
                GwtClientUtils.logLsfViewError("'" + sid + "' is not a grid property marked LSF of a group container '"
                        + container.sID + "' draws, so nothing is drawn per row for it here");
                return;
            }
            placed.mount(sid, host);
            scheduleReconcile();
        }

        @Override
        void unmount(Element host, JavaScriptObject row) {
            if (row != null) { // never mounted: the host holds the mistake mount showed in it
                GwtClientUtils.clearLsfViewError(host);
                return;
            }
            placed.unmount(sid, host);
            scheduleReconcile();
        }
    }
    // ... a renderer per row: the row says which goes in the host, and the grid panel controller of the rows holds
    // them. A row React unmounts - scrolled out, say - sends its renderer back to waiting, it is NOT dropped
    private final class LsfRowPlacement extends LsfPlacement {
        final GPropertyDraw property;

        LsfRowPlacement(String sid, GPropertyDraw property) {
            super(sid);
            this.property = property;
        }

        @Override
        void mount(Element host, JavaScriptObject row) {
            // without the row the host would wait for a single view that is never built - silently, for good
            if (row == null) {
                GwtClientUtils.showLsfViewError(host, "'" + sid + "' is drawn per row, so its <Lsf> needs a row");
                GwtClientUtils.logLsfViewError("'" + sid + "' is an LSF property drawn per row: its <Lsf> is given the"
                        + " row it belongs to, as <Lsf name row/>, since each row has a renderer of its own");
                return;
            }
            GGroupObjectValue rowKey = GGroupObjectValue.resolveObject(row);
            if (rowKey == null) {
                GwtClientUtils.showLsfViewError(host, "the `row` given to '" + sid + "' is not a row");
                GwtClientUtils.logLsfViewError("the `row` passed to '" + sid + "' does not identify a row: pass the row"
                        + " object from the projected data, not its key");
                return;
            }
            // the property, not the sid JSX used: the ledger of who holds which row is the property's own
            if (formController.getGridPanelController(property).place(rowKey, property, host) != null)
                PlacedViews.reportDuplicate(sid, host); // another <Lsf> already placed this property for this row
        }

        @Override
        void unmount(Element host, JavaScriptObject row) {
            GGroupObjectValue rowKey = row != null ? GGroupObjectValue.resolveObject(row) : null;
            if (rowKey != null)
                formController.getGridPanelController(property).unplace(rowKey, property, host);
            else // a host that got no renderer holds the mistake mount showed in it
                GwtClientUtils.clearLsfViewError(host);
        }
    }

    // Tell the server which lsf children this view places, so the data of one it does not place is not read - the way
    // a tab strip tells it which page is active, and a collapse header whether it is collapsed. What is sent is the
    // STATE, worked out from the placements as they stand, never from the mount / unmount that changed them: a child
    // handed to a waiting host is placed however it got there, and a child no <Lsf> ever named is not. A child with a
    // host - holding its view, or waiting for it - is placed; only a change is sent. Settled once the commit that
    // changed the placements is over, after every publication and after a mount that draws, so the first draw settles
    // it too even when it renders no <Lsf> at all
    private void scheduleReconcile() {
        if (reconcilePending)
            return;
        reconcilePending = true;
        int generation = reconcileGeneration;
        GwtClientUtils.afterNextPaint(() -> {
            if (generation != reconcileGeneration) // the root was unmounted since: the form is closing, or re-mounting
                return;
            reconcilePending = false;
            reconcilePlaced();
        });
    }
    private void reconcilePlaced() {
        for (GComponent child : lsfViews) {
            boolean hidden = child.sID == null || !placed.isHeld(child.sID); // no <Lsf> names one with no name
            if (hidden ? reportedHidden.add(child) : reportedHidden.remove(child))
                formController.setUserHidden(child, hidden);
        }
    }

    private void attachView(int index, Element host) {
        ComponentViewWidget childView = getChildView(index);
        childView.appendTo(host);
        // an lsf child is one self-contained view (a property is forced non-inline in PropertyPanelRenderer, so it is
        // a single widget, not inline value/comment siblings) — getSingleWidget is that view. It fills the host as a
        // native SizedFlexPanel child does: fill-parent-flex on the view, and on the host the flex column its lsf-view
        // mark gives it (layout.css) - not fill-parent-flex-cont, which would be written into a node React renders and
        // be lost the moment the component's class for it changes
        GwtClientUtils.addClassName(childView.getSingleWidget().widget.getElement(), "fill-parent-flex");
        resizeChildren(); // the child moved out of the display:none park into a laid-out place
    }

    // the index of a placed lsf component's view among the views this one holds, -1 while it has none
    private int indexOfLsfView(String sid) {
        GComponent component = placeable.get(sid);
        return component != null && component.isLsfView() ? children.indexOf(component) : -1;
    }

    public GContainer getContainer() {
        return container;
    }

    // GReactFormData publishes this container's snapshot after applying a remote or optimistic batch.
    // Lsf captions and images are entries in the same snapshot and follow the same notification.
    public void updateData(JavaScriptObject data) {
        root.updateData(data);
        scheduleReconcile(); // what the view places may change with what it is given
    }

    private void unmount() {
        root.unmount(); // runs the hosts' ref cleanups, which park every mounted child and drop it from `hosts`
        // ... and drops every reconcile asked for so far, the ones those cleanups asked for included: taking the old
        // tree down is not a view hiding anything
        reconcileGeneration++;
        reconcilePending = false;
        placed.reset(); // any host left waiting (its view dropped by SHOWIF) belongs to the old tree; drop it too
        // what the server was told stays known: a view mounted again reports only what changed
    }
}
