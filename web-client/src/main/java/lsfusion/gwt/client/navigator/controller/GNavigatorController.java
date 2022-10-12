package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.*;

public abstract class GNavigatorController implements GINavigatorController {
    private final FormsController formsController;

    private GNavigatorElement root;
    private LinkedHashMap<GNavigatorWindow, GNavigatorView> views = new LinkedHashMap<>();

    public GNavigatorController(FormsController formsController) {
        this.formsController = formsController;
    }

    public void initializeNavigatorViews(List<GNavigatorWindow> windows) {
        for (GNavigatorWindow window : windows) {
            views.put(window, window.createView(this));
        }
    }

    public void setRootElement(GNavigatorElement root) {
        this.root = root;
    }

    public GNavigatorView getNavigatorView(GNavigatorWindow window) {
        return views.get(window);
    }

    @Override
    public void update() {
        Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> result = new HashMap<>();

        for (GNavigatorWindow wind : views.keySet()) {
            result.put(wind, new LinkedHashSet<GNavigatorElement>());
        }

        dfsAddElements(root, null, result);

        Map<GAbstractWindow, Boolean> visibleElements = new HashMap<>();
        for (Map.Entry<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> entry : result.entrySet()) {
            GNavigatorView view = views.get(entry.getKey());
            if (view != null) {
                view.refresh(entry.getValue());
                visibleElements.put(entry.getKey(), !entry.getValue().isEmpty() && entry.getKey().visible);
            }
        }
        updateVisibility(visibleElements);

        for (GNavigatorWindow window : views.keySet()) {
            if (!window.initialSizeSet) {
                setInitialSize(window, views.get(window).getWidth(), views.get(window).getHeight());
            }
        }
    }

    private void dfsAddElements(GNavigatorElement currentElement, GNavigatorWindow currentWindow, Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> result) {
        if ((currentElement.window != null) && (currentElement.window.drawRoot)) {
            result.get(currentElement.window).add(currentElement);
        } else {
            if (currentWindow != null) {
                result.get(currentWindow).add(currentElement);
            }
        }
        GNavigatorWindow nextWindow = currentElement.window == null ? currentWindow : currentElement.window;

        if (currentElement.window == null
                || currentWindow == null
                || currentElement == views.get(currentWindow).getSelectedElement()
                || currentElement.window == currentWindow
                || currentElement.window.drawRoot) {
            for (GNavigatorElement element : currentElement.children) {
                if (!result.get(nextWindow).contains(element)) {
                    dfsAddElements(element, nextWindow, result);
                }
            }
        }
    }

    @Override
    public void openElement(GNavigatorElement element, NativeEvent nativeEvent) {
        if (element instanceof GNavigatorAction) {
            boolean sync = element.asyncExec == null;
            if(!sync)
                element.asyncExec.exec(formsController, null, null, nativeEvent instanceof Event ? (Event) nativeEvent : null, formsController.getDispatcher(),
                        () -> formsController.executeNavigatorAction(element.canonicalName, nativeEvent, sync));
        }
    }
}
