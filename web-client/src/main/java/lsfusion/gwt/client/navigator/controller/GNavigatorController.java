package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.property.async.GAsyncExecutor;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class GNavigatorController implements GINavigatorController {
    private final FormsController formsController;

    private GNavigatorElement root;
    private LinkedHashMap<GNavigatorWindow, GNavigatorView> views = new LinkedHashMap<>();
    // hack, but it's easier to do it this way
    private NativeSIDMap<GNavigatorWindow, Widget> mobileViews = new NativeSIDMap<>();

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

    @Override
    public GNavigatorElement getRoot() {
        return root;
    }

    public void setRoot(GNavigatorElement root) {
        this.root = root;
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
        for (GNavigatorElement element : root.children)
            fillSelectedElements(element, root.window, drawWindow -> false, selectedElements);

        Map<GAbstractWindow, Boolean> visibleElements = new HashMap<>();
        for (Map.Entry<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> entry : selectedElements.entrySet()) {
            GNavigatorView view = views.get(entry.getKey());
            if (view != null) {
                view.refresh(entry.getValue());
                visibleElements.put(entry.getKey(), !entry.getValue().isEmpty() && entry.getKey().visible);
            }
        }
        updateVisibility(visibleElements);
        
        if (firstUpdate) {
            firstUpdate = false;
            for (GNavigatorWindow navigatorWindow : views.keySet()) {
                if (navigatorWindow.isRoot()) {
                    views.get(navigatorWindow).openFirstFolder();
                }
            }
        }
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
                    childDrawWindow -> childDrawWindow != fDrawWindow && views.get(fDrawWindow).getSelectedElement() != currentElement,
                    result);
        }
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

    public void onSelectedElement(GNavigatorElement selectedElement) {
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

    public void resetSelectedElements(GNavigatorElement newSelectedElement) {
        for (GNavigatorView value : views.values()) {
            value.resetSelectedElement(newSelectedElement);
        }
    }
}
