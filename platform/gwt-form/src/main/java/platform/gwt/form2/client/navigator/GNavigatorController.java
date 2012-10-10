package platform.gwt.form2.client.navigator;

import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GAbstractWindow;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public abstract class GNavigatorController implements GINavigatorController {
    private LinkedHashMap<GNavigatorWindow, GNavigatorView> views = new LinkedHashMap<GNavigatorWindow, GNavigatorView>();
    public GNavigatorElement root;

    public void initializeNavigatorViews(GNavigatorWindow[] windows) {
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
            setInitialSize(window, views.get(window).getWidth(), views.get(window).getHeight());
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
            if (form.modalityType.isModal()) {
                OpenFormEvent.fireEvent(form.sid, form.caption);
            } else {
                OpenFormEvent.fireEvent(form.sid, form.caption);
            }
        } else if (element instanceof GNavigatorAction) {
//            openAction((GNavigatorAction) element);
        }
    }
}
