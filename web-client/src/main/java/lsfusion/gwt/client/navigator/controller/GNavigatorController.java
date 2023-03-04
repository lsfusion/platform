package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.property.async.GAsyncExecutor;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.*;
import java.util.function.Function;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

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

        // looking for "active" (selected) elements
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

        autoSizeWindows();
    }

    private void dfsAddElements(GNavigatorElement currentElement, GNavigatorWindow currentWindow, Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> result) {
        GNavigatorWindow parentWindow = nvl(currentElement.window, currentWindow);
        GNavigatorWindow window = currentElement.parentWindow ? parentWindow : currentWindow;
        if(window != null) {
            result.get(window).add(currentElement);
        }

        if (currentElement.window == null
                || currentWindow == null
                || currentWindow.isSystem()
                || currentElement == views.get(window).getSelectedElement()
                || currentElement.window == currentWindow) {
            for (GNavigatorElement element : currentElement.children) {
                if (!result.get(parentWindow).contains(element)) {
                    dfsAddElements(element, parentWindow, result);
                }
            }
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

    public void resetSelectedElements(GNavigatorElement newSelectedElement) {
        for (GNavigatorView value : views.values()) {
            value.resetSelectedElement(newSelectedElement);
        }
    }
}
