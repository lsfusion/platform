package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GNavigatorScheduler;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorSchedulerAction;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.property.async.GAsyncExecutor;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.view.GAbstractNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class GNavigatorController implements GINavigatorController {
    private final FormsController formsController;

    private GNavigatorElement root;
    private LinkedHashMap<GNavigatorWindow, GAbstractNavigatorView> views = new LinkedHashMap<>();
    // hack, but it's easier to do it this way
    private NativeSIDMap<GNavigatorWindow, Widget> mobileViews = new NativeSIDMap<>();
    // kept here and not in the views, since a window's contents are the subtree of the selected element of ANOTHER
    // window - a view owning it would let one renderer empty every window hanging off it
    private NativeSIDMap<GNavigatorWindow, GNavigatorElement> selected = new NativeSIDMap<>();

    public GNavigatorController(FormsController formsController) {
        this.formsController = formsController;
    }

    public void initializeNavigatorViews(List<GNavigatorWindow> windows) {
        for (GNavigatorWindow window : windows) {
            views.put(window, window.createView(this));
        }
    }
    public void initMobileNavigatorView(GNavigatorWindow window, Widget widget) {
        mobileViews.put(window, widget);
    }

    public void initializeNavigatorSchedulers(List<GNavigatorScheduler> navigatorSchedulers) {
        for(GNavigatorScheduler navigatorScheduler : navigatorSchedulers) {
            scheduleNavigatorScheduler(navigatorScheduler);
        }
    }

    private void scheduleNavigatorScheduler(GNavigatorScheduler navigatorScheduler) {
        Scheduler.get().scheduleFixedPeriod(() -> {
            executeNavigatorSchedulerAction(navigatorScheduler, () -> {
                if (!navigatorScheduler.fixed) {
                    scheduleNavigatorScheduler(navigatorScheduler);
                }
            });
            return navigatorScheduler.fixed;
        }, navigatorScheduler.period * 1000);
    }

    private void executeNavigatorSchedulerAction(GNavigatorScheduler navigatorScheduler, Runnable onSuccess) {
        MainFrame.syncDispatch(new ExecuteNavigatorSchedulerAction(navigatorScheduler), new GwtActionDispatcher.ServerResponseCallback(false) {
            @Override
            protected GwtActionDispatcher getDispatcher() {
                return formsController.getDispatcher();
            }

            @Override
            protected Runnable getOnRequestFinished() {
                if(onSuccess != null)
                    return onSuccess;
                return super.getOnRequestFinished();
            }
        }, false);
    }

    @Override
    public GNavigatorElement getRoot() {
        return root;
    }

    private JavaScriptObject controller;

    public void setController(JavaScriptObject controller) {
        this.controller = controller;
    }

    @Override
    public JavaScriptObject getController() {
        return controller;
    }

    public void setRoot(GNavigatorElement root) {
        this.root = root;
    }

    // the root itself is skipped: it is the synthetic top of the tree, not something addressable by name
    public GNavigatorElement getElement(String canonicalName) {
        return getElement(root, canonicalName);
    }

    private GNavigatorElement getElement(GNavigatorElement parent, String canonicalName) {
        for (GNavigatorElement child : parent.children) {
            if (child.canonicalName.equals(canonicalName))
                return child;

            GNavigatorElement element = getElement(child, canonicalName);
            if (element != null)
                return element;
        }
        return null;
    }

    public Widget getNavigatorWidgetView(GNavigatorWindow window) {
        if(MainFrame.mobile)
            return mobileViews.get(window);

        return views.get(window).getView();
    }

    boolean firstUpdate = true;
    @Override
    public void update() {
        Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> selectedElements = new HashMap<>();

        for (GNavigatorWindow wind : views.keySet()) {
            selectedElements.put(wind, new LinkedHashSet<GNavigatorElement>());
        }

        // looking for "active" (selected) elements
        if(root != null) {
            for (GNavigatorElement element : root.children)
                fillSelectedElements(element, root.window, drawWindow -> false, selectedElements);
        }

        Map<GAbstractWindow, Boolean> visibleElements = new HashMap<>();
        for (Map.Entry<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> entry : selectedElements.entrySet()) {
            GAbstractNavigatorView view = views.get(entry.getKey());
            if (view != null) {
                view.refresh(entry.getValue(), selected.get(entry.getKey()));
                // a standard strip with nothing to draw is hidden, but a custom one is given the empty bucket and
                // decides for itself - it is the only place an application can draw "pick a section", loading, and such
                visibleElements.put(entry.getKey(), entry.getKey().visible && (entry.getKey().isCustom() || !entry.getValue().isEmpty()));
            }
        }
        updateVisibility(visibleElements);

        if (firstUpdate) {
            firstUpdate = false; // before activating, since activate() runs update() again
            for (GNavigatorWindow navigatorWindow : views.keySet()) {
                if (navigatorWindow.isRoot()) {
                    GNavigatorElement firstFolder = getFirstFolder(selectedElements.get(navigatorWindow));
                    if (firstFolder != null)
                        activate(firstFolder, null);
                }
            }
        }
    }

    // deliberately not skipping hidden elements: the folder is picked before the view's hide check, so a hidden
    // first folder is opened at start as well
    private GNavigatorElement getFirstFolder(LinkedHashSet<GNavigatorElement> elements) {
        for (GNavigatorElement element : elements)
            if (element.isFolder() && !elements.contains(element.parent)) // only root elements, children are drawn recursively
                return element;
        return null;
    }

    private void fillSelectedElements(GNavigatorElement currentElement, GNavigatorWindow drawWindow, Predicate<GNavigatorWindow> checkNotSelected, Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> result) {
        GNavigatorWindow drawChildrenWindow = drawWindow;

        if(currentElement.window != null) {
            drawChildrenWindow = currentElement.window;
            if (currentElement.parentWindow)
                drawWindow = drawChildrenWindow;
        }

        if(checkNotSelected.test(drawWindow))
            return;

        result.get(drawWindow).add(currentElement);

        final GNavigatorWindow fDrawWindow = drawWindow;
        for (GNavigatorElement element : currentElement.children) {
            fillSelectedElements(element, drawChildrenWindow,
                    // if window has changed and the parent element is not selected - we're not drawing the child element
                    childDrawWindow -> childDrawWindow != fDrawWindow && selected.get(fDrawWindow) != currentElement,
                    result);
        }
    }

    @Override
    public void activate(GNavigatorElement element, NativeEvent nativeEvent) {
        if (element.isFolder()) {
            resetSelectedElements(element);
            selected.put(element.getDrawWindow(), element);

            update();

            onSelectedElement(element);
        } else
            openElement((GNavigatorAction) element, nativeEvent);
    }

    @Override
    public void openElement(GNavigatorAction element, NativeEvent nativeEvent) {
        if (element instanceof GNavigatorAction) {
            boolean sync = element.asyncExec == null;
            Function asyncExec = pushAsyncResult -> formsController.executeNavigatorAction(element.canonicalName, nativeEvent, sync);
            if(sync) {
                asyncExec.apply(null);
            } else {
                element.asyncExec.exec(formsController, null, null, nativeEvent instanceof Event ? (Event) nativeEvent : null, new GAsyncExecutor(formsController.getDispatcher(), asyncExec));
            }
        }
    }

    private void onSelectedElement(GNavigatorElement selectedElement) {
        NativeSIDMap<GNavigatorWindow, Boolean> childrenWindows = new NativeSIDMap<>();
        for (GNavigatorElement element : selectedElement.children) {
            GNavigatorWindow elementWindow = element.getDrawWindow();
            if(elementWindow != null)
                childrenWindows.put(elementWindow, true);
        }
        childrenWindows.foreachKey(child -> views.get(child).onParentSelected());

        GNavigatorWindow selectedWindow = selectedElement.getDrawWindow();
        if(selectedWindow != null)
            views.get(selectedWindow).onSelected();
    }

    // an ancestor (or self) is kept, so that switching to a deep element collapses the sibling branches with their
    // descendant windows, but not the chain leading to that element
    private void resetSelectedElements(GNavigatorElement newSelectedElement) {
        List<GNavigatorWindow> reset = new ArrayList<>();
        selected.foreachEntry((window, element) -> {
            if (element.findChild(newSelectedElement) == null)
                reset.add(window);
        });
        for (GNavigatorWindow window : reset)
            selected.remove(window);
    }
}
