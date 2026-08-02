package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.base.Custom;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;

public class CheckNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement ne) {
        if (ne.isTopFolderWithoutWindow()) {
            throw new RuntimeException("Navigator folder " + ne + " in the top NAVIGATOR must specify a WINDOW for its children");
        }
        // only a React component both draws elements itself and can place a standard drawing; anywhere else the
        // platform draws every element anyway, so an LSF that reached such a window is a mistake, as it is on a form.
        // Checked here and not while the module is read: EXTEND WINDOW may give the window its component later
        if (ne.lsf && !isReactWindow(ne)) {
            throw new RuntimeException("LSF is set for navigator element " + ne + ", whose window is not drawn by a CUSTOM React component");
        }

        checkTemplatePlaces(ne);
    }

    // a place in a window's TEMPLATE names an element by its canonical name, resolved when the module was read - but
    // WHICH window draws that element is only settled once every module has run, since an extending one may move it.
    // So it is asked here: a place naming an element another window draws is never filled, and the browser is a late
    // place to find that out about a literal the server has been holding all along
    // read once per window and not once per element: this runs for every navigator element, and a template can be as
    // long as an application's markup
    private final Map<NavigatorWindow, Set<String>> templatePlaces = new ConcurrentHashMap<>();

    private void checkTemplatePlaces(NavigatorElement ne) {
        NavigatorWindow window = ne.getDrawWindow();
        for (AbstractWindow any : getBL().getWindows()) {
            if (any == window || !(any instanceof NavigatorWindow) || any.isReact())
                continue;

            NavigatorWindow other = (NavigatorWindow) any;
            String custom = other.getCustom();
            if (custom != null && templatePlaces.computeIfAbsent(other, w -> Custom.places(custom)).contains(ne.getCanonicalName()))
                throw new RuntimeException("the template of window " + other.getCanonicalName() + " gives a place to navigator element "
                        + ne + ", which is drawn in window " + (window != null ? window.getCanonicalName() : "none")
                        + ", so nothing would ever be placed there");
        }
    }

    private boolean isReactWindow(NavigatorElement ne) {
        NavigatorWindow window = ne.getDrawWindow();
        return window != null && window.isReact();
    }

    public String getCaption() {
        return "Checking navigator elements";
    }
}
