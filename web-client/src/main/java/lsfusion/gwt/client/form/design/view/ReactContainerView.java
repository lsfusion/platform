package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.List;

// custom = 'Name': a React component (window[Name], Name = the container's custom) draws this container's subtree
// from the form's projection - its props { data, controller } (GReactFormData), which the window.lsfusion hooks read
// too - except the platform-drawn components it places, the ones with a view of their own (`custom = NULL`, NOCUSTOM, a
// custom of their own): those keep their GWT view, and the component places each with <Platform name/>, a
// platform-drawn grid property with <Platform name row/>, one renderer per row
public class ReactContainerView extends ParkedContainerView {

    private final ReactRoot root;
    // this container's state in the form's projection: what its `props.data` is built from, and its `props.controller`
    private final GReactFormData.ViewState state;

    // the components this view places, by the name a <Platform> gives them (getName): those it is the React place of (GComponent.getReactPlace, the rule the
    // layout and the projection ask too) - its children and what is inside the containers it draws itself. What a
    // <Platform> name means is looked up here
    private final NativeStringMap<GComponent> placeable = new NativeStringMap<>();
    // ... and of them the platform-drawn ones, placed whole, one view each, named or not: what the server is told of
    // (reconcilePlaced)
    private final List<GComponent> platformViews = new ArrayList<>();
    // ... and the containers it draws itself, named, each with whether the component shows it (useShown): the server
    // is told of them too - a container is shown until the component says otherwise
    private final NativeSIDMap<GComponent, Boolean> drawnShown = new NativeSIDMap<>();

    // name -> the host claimed for it (the first one wins). The child's view is mounted there once it exists, so
    // the host outlives a SHOWIF drop/rebuild of the view: whether the view exists now is indexOfPlatformView(sid) >= 0
    private final PlacedViews<String> placed = new PlacedViews<>(new PlacedViews.Views<String>() {
        @Override
        public PlacedViews.Problem problem(String sid) {
            GComponent component = placeable.get(sid);
            if (component == null)
                return new PlacedViews.Problem("'" + sid + "' is not inside '" + container.sID + "'",
                        "component '" + sid + "' is not inside container '" + container.sID + "', nor in a container it draws itself");
            if (!component.isPlatformDrawn()) // inside the container, with no view of its own: React draws it
                return new PlacedViews.Problem("'" + sid + "' has no view of its own",
                        "'" + sid + "' inside container '" + container.sID + "' has no view of its own, so the view draws"
                                + " it from `data`: give it `custom = NULL` for the platform to draw it, and place it with <Platform>");
            return null;
        }

        @Override
        public boolean place(String sid, Element host) {
            int index = indexOfPlatformView(sid);
            if (index < 0) // the view does not exist yet - a SHOWIF dropped it, or it is not built - so the host waits
                return false;

            attachView(index, host);
            return true;
        }

        @Override
        public void park(String sid) {
            int index = indexOfPlatformView(sid);
            if (index >= 0) // if a SHOWIF already dropped the view there is nothing to park
                parkChild(index);
        }

        @Override
        public void resized(String sid) {
            resizeChildren();
        }
    });
    // what the server has been told about the platform-drawn children: the SET this view places now, not the events
    // that led to it (reconcilePlaced). The children it was told are hidden - a child the server was told nothing about
    // is shown, the server's default
    private final NativeSIDMap<GComponent, Boolean> reportedHidden = new NativeSIDMap<>();
    // whether the component's tree is there (ReactRoot.Placement.drawn / gone): only then is there anything to tell
    private boolean drawn;
    // a pending reconcile of an older generation is dropped: the tree was taken down since
    private int reconcileGeneration;
    private boolean reconcilePending;

    public ReactContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        addPlaceable(container);
        // THIS projection's controller: it names what this container's own `props.data` carries and nothing else -
        // a view that cannot see its neighbour does not name it either. The classic surfaces (a custom group view, a
        // custom cell editor, an INTERNAL CLIENT action) keep the form's own controller
        state = formController.addReactView(container, this::updateData);
        root = new ReactRoot(container.getCustom(), state.controller, new ReactViewPlacement(formController, state) {
            // a name given no row: one view of what this view places
            @Override
            PlatformPlacement placeView(String name) {
                return new PlatformViewPlacement(name);
            }

            // the entry a name keys: a property's where its name says - data.o.note -, any other component's descriptor
            // under its design name, the field of that name
            @Override
            public JavaScriptObject entryOf(JavaScriptObject data, String name) {
                GComponent component = placeable.get(name);
                return component instanceof GPropertyDraw ? state.entryOf(data, (GPropertyDraw) component) : super.entryOf(data, name);
            }

            // whether the component shows a container it draws itself - kept, and the server told with the placements
            @Override
            public void show(String name, boolean shown) {
                GComponent component = placeable.get(name);
                if (component == null || !drawnShown.containsKey(component)) {
                    GwtClientUtils.logCustomViewError("useShown('" + name + "'): '" + name + "' is not a container '"
                            + container.sID + "' draws itself - one the platform draws is shown by placing it with"
                            + " <Platform>");
                    return;
                }
                if (drawnShown.put(component, shown) != shown)
                    scheduleReconcile();
            }

            // drawn now: a tree mounted again settles what it places too, even when that is nothing
            @Override
            public void drawn() {
                drawn = true;
                scheduleReconcile();
            }

            // the tree is going, and React runs its hosts' ref cleanups next, which park every mounted child and
            // drop its host: taking the old tree down is not a view hiding anything, so nothing they ask to be told
            // is, nor any reconcile asked for so far. Nothing is parked here: a development React takes a tree down
            // and draws it again where it is, running no ref in between (StrictMode), and what it placed stays placed.
            // What the server was told stays known: a view mounted again reports only what changed
            @Override
            public void gone() {
                drawn = false;
                reconcileGeneration++;
                reconcilePending = false;
            }
        });
        formController.getFormLayout().mountReactView(root, panel, panel.getElement());
    }

    // what is under the container whose React place it is: the design is fixed, so this is taken once. Not a grid
    // property: it is no view of its own here - a platform-drawn one's renderers are placed per row by the view that
    // draws its group's rows (ReactViewPlacement), any other's cells are drawn there - and it is parked in its group's box,
    // which says nothing about where its rows are, so its name would claim a place a panel property of the same name
    // has
    private void addPlaceable(GComponent component) {
        for (GComponent child : component.getChildren()) {
            if (child.getReactPlace() == container && !(child instanceof GPropertyDraw && ((GPropertyDraw) child).isList)) {
                String name = GReactFormData.getName(child);
                if (name != null) // what a <Platform> name can mean
                    placeable.put(name, child);
                if (child.isPlatformDrawn())
                    platformViews.add(child);
                else if (child instanceof GContainer && name != null)
                    drawnShown.put(child, true);
            }
            addPlaceable(child);
        }
    }

    // React puts each view where a <Platform> names it, so the order the views are held in means nothing: a new one
    // goes last. This view holds the views of what it places at any depth, not only of its container's own children
    @Override
    protected int getInsertIndex(GComponent child) {
        return children.size();
    }

    @Override
    protected void addImpl(int index) {
        // React owns the subtree, so only a platform-drawn child gets a GWT view here
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
        placed.viewRemoved(GReactFormData.getName(children.get(index)), true);

        super.removeImpl(index);
    }

    // one view, mounted in the host claimed for it (`placed`)
    private final class PlatformViewPlacement extends ReactViewPlacement.PlatformPlacement {
        final String name;

        PlatformViewPlacement(String name) {
            this.name = name;
        }

        @Override
        void mount(Element host) {
            placed.mount(name, host);
            scheduleReconcile();
        }

        @Override
        void unmount(Element host) {
            placed.unmount(name, host);
            scheduleReconcile();
        }
    }
    // Tell the server which platform-drawn children this view places, so the data of one it does not place is not read
    // - the way a tab strip tells it which page is active, and a collapse header whether it is collapsed. What is sent
    // is the STATE, worked out from the placements as they stand, never from the mount / unmount that changed them: a
    // child handed to a waiting host is placed however it got there, and a child no <Platform> ever named is not. A
    // child with a host - holding its view, or waiting for it - is placed; only a change is sent. And which containers
    // it draws itself it shows (useShown), the same way. Settled once the commit that changed the placements is over,
    // after every publication and after a mount that draws, so the first draw settles it too even when it renders no
    // <Platform> at all
    private void scheduleReconcile() {
        if (!drawn || reconcilePending)
            return;
        reconcilePending = true;
        int generation = reconcileGeneration;
        GwtClientUtils.afterNextPaint(() -> {
            if (generation != reconcileGeneration) // the tree was taken down since: the form is closing, or the view
                return;                            // is not placed - a tree drawn again asks anew
            reconcilePending = false;
            reconcilePlaced();
        });
    }
    private void reconcilePlaced() {
        for (GComponent child : platformViews) {
            String name = GReactFormData.getName(child);
            report(child, name == null || !placed.isHeld(name)); // no <Platform> names one with no name
        }
        drawnShown.foreachEntry((child, shown) -> report(child, !shown));
    }
    private void report(GComponent component, boolean hidden) {
        if (hidden ? reportedHidden.put(component, true) == null : reportedHidden.remove(component) != null)
            formController.setUserHidden(component, hidden);
    }

    private void attachView(int index, Element host) {
        ComponentViewWidget childView = getChildView(index);
        childView.appendTo(host);
        formController.getFormLayout().viewMoved();
        // a platform-drawn child is one self-contained view (a property is forced non-inline in PropertyPanelRenderer,
        // so it is a single widget, not inline value/comment siblings) — getSingleWidget is that view. It fills the
        // host as a native SizedFlexPanel child does: fill-parent-flex on the view, and on the host the flex column its
        // lsf-platform mark gives it (layout.css) - not fill-parent-flex-cont, which would be written into a node React
        // renders and be lost the moment the component's class for it changes
        GwtClientUtils.addClassName(childView.getSingleWidget().widget.getElement(), "fill-parent-flex");
        resizeChildren(); // the child moved out of the display:none park into a laid-out place
    }

    // the index of a placed platform-drawn component's view among the views this one holds, -1 while it has none
    private int indexOfPlatformView(String sid) {
        GComponent component = placeable.get(sid);
        return component != null && component.isPlatformDrawn() ? children.indexOf(component) : -1;
    }

    public GContainer getContainer() {
        return container;
    }

    // GReactFormData publishes this container's snapshot after applying a remote or optimistic batch. The captions and
    // images of platform-drawn children are entries in the same snapshot and follow the same notification.
    public void updateData(JavaScriptObject data) {
        root.updateData(data);
        scheduleReconcile(); // what the view places may change with what it is given
    }
}
