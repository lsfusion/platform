package lsfusion.gwt.form.client.navigator;

import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.shared.view.GNavigatorElement;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;
import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;

import java.util.*;

public abstract class GNavigatorController implements GINavigatorController {
    private final FormsController formsController;

    private GNavigatorElement root;
    private LinkedHashMap<GNavigatorWindow, GNavigatorView> views = new LinkedHashMap<GNavigatorWindow, GNavigatorView>();

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
        Map<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> result = new HashMap<GNavigatorWindow, LinkedHashSet<GNavigatorElement>>();

        for (GNavigatorWindow wind : views.keySet()) {
            result.put(wind, new LinkedHashSet<GNavigatorElement>());
        }

        dfsAddElements(root, null, result);

        Map<GAbstractWindow, Boolean> visibleElements = new HashMap<GAbstractWindow, Boolean>();
        for (Map.Entry<GNavigatorWindow, LinkedHashSet<GNavigatorElement>> entry : result.entrySet()) {
            GNavigatorView view = views.get(entry.getKey());
            if (view != null) {
                view.refresh(entry.getValue());
                visibleElements.put(entry.getKey(), !entry.getValue().isEmpty());
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
    public void openElement(GNavigatorElement element) {
        if (element instanceof GNavigatorForm) {
            GNavigatorForm form = (GNavigatorForm) element;

            formsController.openForm(form.sid, form.modalityType);
        } else if (element instanceof GNavigatorAction) {
            formsController.executeNavigatorAction((GNavigatorAction) element);
        }
    }
}
