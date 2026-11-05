package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;

import java.util.ArrayList;
import java.util.List;

// NAVIGATOR CUSTOM 'AppLayout': a React component lays the windows out instead of the standard splits and tabs. It never
// draws a window: it renders a host per window it wants shown, and the platform moves that window's own view in - a
// navigator window's buttons, a FORMS window's forms, the log's messages, drawn as without a component, the window's own
// CUSTOM included -, the same crossing <Platform name/> makes for a design child, a navigator element, an open form and a
// logged message.
//
// A window the component places nowhere is not shown and lives on in the park, as a form the forms window's component
// does not place: its buttons refresh, its forms stay open, the log logs. What the component is told of a window is what
// places and labels it, and the rest of the standard layout - the sizes the user drags, the tabs of windows sharing a
// place - is the component's own
public class ReactWindowElement extends WindowElement {

    private final ResizableComplexPanel panel = new ResizableComplexPanel();
    private final ReactRoot root;

    // every window of the navigator - System.forms too, which the standard layout puts in its centre on its own - under
    // the canonical name <Platform> places it by
    private final NativeStringMap<SimpleWindowElement> byName = new NativeStringMap<>();

    // where a window's view lives while nothing places it. It has to stay in the document and stay a logical child of
    // the panel, or GWT would detach the widget; the node itself must be OUTSIDE the panel, because React clears its
    // own root. display:none, as a background tab is
    private final Element park = Document.get().createDivElement();

    private final PlacedViews placed = new PlacedViews(new PlacedViews.Views() {
        @Override
        public PlacedViews.Problem problem(String name) {
            return byName.get(name) != null ? null : new PlacedViews.Problem("'" + name + "' is not a window",
                    "'" + name + "' is not a window of the navigator, so nothing will ever be placed here");
        }

        @Override
        public boolean place(String name, Element host) {
            Widget view = byName.get(name).getView();
            host.appendChild(view.getElement());
            GwtClientUtils.addClassName(view, "fill-parent-flex"); // the host is a flex column by its mark (layout.css)
            return true;
        }

        @Override
        public void park(String name) {
            park.appendChild(byName.get(name).getView().getElement()); // still attached and alive, ready to be placed again
        }
    });

    // the projection: an entry per window under its canonical name, and nothing else - no state of the arrangement is
    // the platform's
    private JavaScriptObject data = JavaScriptObject.createObject();

    public ReactWindowElement(WindowsController controller, String custom, List<GAbstractWindow> allWindows, GAbstractWindow formsWindow) {
        super(controller, 0, 0, 100, 100);

        List<GAbstractWindow> navigatorWindows = new ArrayList<>(allWindows);
        navigatorWindows.add(formsWindow);
        for (GAbstractWindow window : navigatorWindows) {
            SimpleWindowElement element = new SimpleWindowElement(controller, window, window.x, window.y, window.width, window.height);
            element.parent = this;
            byName.put(window.canonicalName, element);
        }

        // the crossing BACK to the platform: the component renders the host node, we move the window's own view in. A
        // window's host holds the window's view, which the layout fills it with by its marks. Nothing about the
        // arrangement is the platform's to do, so the controller has nothing on it
        root = new ReactRoot(custom, JavaScriptObject.createObject(), placed.placement(new ReactRoot.MarkedHost()));

        park.getStyle().setDisplay(Style.Display.NONE);
        Document.get().getBody().appendChild(park);

        panel.addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(panel.getElement(), false); // never measured, so it stays asynchronous
            else
                root.unmount();
        });
    }

    @Override
    public void initializeView(WindowsController controller) {
        byName.foreachValue(window -> {
            window.initializeView(controller); // registered, so its visibility reaches this element

            Widget view = window.getView();
            panel.addToElement(view, park); // a logical child of the panel, parked in the DOM until the component says where it goes
            // the view hears that its size changed - the component re-lays itself out on its own, and after any onResize
            // the platform sends, while a grid, a pivot's chart and a map measure themselves on onResize, as a split's
            // child does when its border is dragged. Watched on the view, which fills its host, so that neither the
            // hosts nor the park need any bookkeeping: a view placed or parked changes size too
            if (view instanceof RequiresResize)
                observeResize(view.getElement(), ((RequiresResize) view)::onResize);

            // shown as the standard layout shows it to begin with, so the declared visibility that follows changes nothing
            addEntry(data, window.window.canonicalName, window.window.caption, !window.window.visible, window.window.vertical);
        });
        // every window is known now, and the component is drawn from that
        root.updateData(data);
    }

    @Override
    public void onAddView(WindowsController controller) {
        byName.foreachValue(window -> window.onAddView(controller));
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        setHidden(window, false);
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        setHidden(window, true);
    }

    // whether the standard layout would show the window: HIDE WINDOW, or a navigator window with nothing to draw. The
    // snapshot is the same one when nothing changed, and the root skips it
    private void setHidden(WindowElement window, boolean hidden) {
        data = withHidden(data, window.getSID(), hidden);
        root.updateData(data);
    }

    @Override
    public void addElement(WindowElement window) {
        throw new UnsupportedOperationException(); // its windows are every window of the navigator, taken when it is built
    }

    @Override
    public String getCaption() {
        return null;
    }

    @Override
    public Widget getView() {
        return panel;
    }

    @Override
    public boolean isAutoSize(boolean vertical) {
        return false;
    }

    @Override
    public String getSID() {
        return null; // a SID keys the sizes a split keeps for its children, and this is the root
    }

    private static native void observeResize(Element view, Runnable resized)/*-{
        new $wnd.ResizeObserver(function () {
            resized.@java.lang.Runnable::run()();
        }).observe(view);
    }-*/;

    // `name` is on the entry itself, so an entry passed around alone still knows its window, as on the entries of the
    // other windows' projections. `vertical`: the window draws its buttons in a column - its own orientation, and the
    // vertical navbar setting turns logo / root / system so whoever lays them out
    private static native void addEntry(JavaScriptObject data, String name, String caption, boolean hidden, boolean vertical)/*-{
        data[name] = { name: name, caption: caption, hidden: hidden, vertical: vertical };
    }-*/;

    // structural sharing, the guarantee every projection gives: a copy in which only this window's entry is new, so an
    // entry that did not change keeps its reference - and the same snapshot when nothing changed at all
    private static native JavaScriptObject withHidden(JavaScriptObject data, String name, boolean hidden)/*-{
        var entry = data[name];
        if (entry.hidden === hidden)
            return data;

        var changed = @lsfusion.gwt.client.base.GwtClientUtils::copyObject(Lcom/google/gwt/core/client/JavaScriptObject;)(entry);
        changed.hidden = hidden;
        var next = @lsfusion.gwt.client.base.GwtClientUtils::copyObject(Lcom/google/gwt/core/client/JavaScriptObject;)(data);
        next[name] = changed;
        return next;
    }-*/;
}
