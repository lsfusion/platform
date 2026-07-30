package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;
import java.util.function.BiConsumer;

// everything a navigator window's view has whatever draws it: the widget it IS for the layout, the classes the
// navigator propagates through it, and the walk that says which elements it draws and how deep each one sits
public abstract class GAbstractNavigatorView {

    protected final GNavigatorWindow window;
    private final Widget view;

    private final RecentlyEventClassHandler parentRecentlySelected;
    private final RecentlyEventClassHandler recentlySelected;

    protected GAbstractNavigatorView(GNavigatorWindow window, Widget view) {
        this.window = window;
        this.view = view;

        GFormLayout.setDebugInfo(view, window.canonicalName);

        // we want to propagate this classes, since window hover classes are also propagated
        parentRecentlySelected = new RecentlyEventClassHandler(view, true, "parent-was-selected-recently", 2000);
        recentlySelected = new RecentlyEventClassHandler(view, true, "was-selected-recently", 1000);
    }

    // the same widget for the window's lifetime: the window layout writes layout data onto that exact widget, and
    // WindowsController.updateElementClass adds and removes classes by traversing from it
    public Widget getView() {
        return view;
    }

    // the only place where server-pushed caption / image / elementClass / hide reach the DOM, since on desktop there
    // is no per-element notification, just one global update - which is reentrant, so refresh must not call it back
    public abstract void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected);

    public void onParentSelected() {
        parentRecentlySelected.onEvent();
    }

    public void onSelected() {
        recentlySelected.onEvent();
    }

    // which elements of the bucket this window draws, in which order, and at what depth - asked once here, because a
    // custom window hands out the standard buttons and they must sit at the level the standard panel would give them.
    // A hidden element is skipped and not descended into either, so a child of a hidden folder is not drawn on its own
    static void forEachDrawn(GNavigatorWindow window, LinkedHashSet<GNavigatorElement> elements, BiConsumer<GNavigatorElement, Integer> drawn) {
        for (GNavigatorElement element : elements)
            if (!elements.contains(element.parent)) // only root elements, the rest are reached from them
                drawElement(window, element, elements, 0, drawn);
    }

    private static void drawElement(GNavigatorWindow window, GNavigatorElement element, LinkedHashSet<GNavigatorElement> elements, int level, BiConsumer<GNavigatorElement, Integer> drawn) {
        if (element.hide)
            return;

        drawn.accept(element, level);

        if (element.window == null || element.window.equals(window))
            for (GNavigatorElement child : element.children)
                if (elements.contains(child))
                    drawElement(window, child, elements, level + 1, drawn);
    }
}
