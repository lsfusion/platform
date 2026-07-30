package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// the standard buttons of a window whose view draws the elements itself, and the park they wait in - the navigator's
// half of what ParkedContainerView is for a form container. A view that places them - into the places of an HTML
// template, or into the nodes a React component renders - gets the platform's own button, with its icon, caption,
// tooltip, classes and click behaviour, instead of reproducing them.
public class NavigatorButtons {

    private final GNavigatorWindow window;
    private final GINavigatorController navigatorController;
    private final ResizableComplexPanel panel;

    // where a button lives while nothing places it. It has to stay in the document and stay a logical child of the
    // window's panel, or GWT would detach the widget and destroy its tooltip; the node itself must be OUTSIDE the
    // panel, because both drawing paths overwrite the panel's content (React clears its root, a template its innerHTML)
    private final Element park = Document.get().createDivElement();

    private final NativeStringMap<NavigatorImageButton> buttons = new NativeStringMap<>();
    private final NativeStringMap<Element> hosts = new NativeStringMap<>(); // where a button was asked for, drawn or not
    private final NativeStringMap<Element> filled = new NativeStringMap<>(); // where one actually sits
    // the names of the LAST refresh, and the only ones that may be placed: an element the selection took out of the
    // window is gone from the navigator, so handing its button out would activate an element the window no longer draws
    private final List<String> drawnNames = new ArrayList<>();

    public NavigatorButtons(GNavigatorWindow window, GINavigatorController navigatorController, ResizableComplexPanel panel) {
        this.window = window;
        this.navigatorController = navigatorController;
        this.panel = panel;

        park.getStyle().setDisplay(Style.Display.NONE);
        RootPanel.getBodyElement().appendChild(park);
    }

    // the same walk the standard panel makes, so a placed button carries the level class it would carry there
    public void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected) {
        drawnNames.clear();
        GAbstractNavigatorView.forEachDrawn(window, elements, (element, level) -> {
            drawnNames.add(element.canonicalName);
            ensureButton(element, level, window.isActive(element, selected));
        });

        removeGone();
        fillAsked();
    }

    private void ensureButton(GNavigatorElement element, int level, boolean active) {
        NavigatorImageButton button = buttons.get(element.canonicalName);
        if (button == null) {
            button = new NavigatorImageButton(element, window.hasVerticalTextPosition(), level, active, navigatorController::activate);
            buttons.put(element.canonicalName, button);
            panel.addToElement(button, park); // a logical child of the panel, parked in the DOM
        } else
            button.change(element, level, active); // caption / image / class / hide are written into the element in
                                                   // place, and the level or the active state may have changed too
    }

    // a button of an element the window no longer draws is dropped whole, so nothing keeps the element it was built
    // from alive and no later render can place it
    private void removeGone() {
        List<String> gone = new ArrayList<>();
        buttons.foreachKey(name -> {
            if (!drawnNames.contains(name))
                gone.add(name);
        });

        for (String name : gone) {
            filled.remove(name);
            panel.remove(buttons.remove(name));
        }
    }

    // a component renders the same <Lsf> until its own props change, so a place is remembered whether or not the
    // window draws that element right now - given up on here, it would never be offered again, and the element would
    // stay missing once the selection brought it into the window. A form container waits the same way for a child
    // whose view a SHOWIF has dropped
    public void mountButton(String canonicalName, Element host) {
        GNavigatorElement element = navigatorController.getElement(canonicalName);
        if (element == null) { // a name no element has will never be drawn, so it is shown in the page, as a form's is
            GwtClientUtils.showLsfViewError(host, "'" + canonicalName + "' is not a navigator element");
            GwtClientUtils.logLsfViewError("'" + canonicalName + "' is not a navigator element, so nothing will ever be placed here");
            return;
        }
        if (!element.lsf) { // declared, but the component owns it: there is no drawing to place
            GwtClientUtils.showLsfViewError(host, "'" + canonicalName + "' has no LSF");
            GwtClientUtils.logLsfViewError("'" + canonicalName + "' has no LSF, so the component draws it from the projection instead of placing it");
            return;
        }
        // whether the window draws it RIGHT NOW depends on the selection and is nobody's mistake, but which window
        // draws it at all does not - and waiting for a button another window owns would wait forever, in silence
        if (element.getDrawWindow() != window) {
            GwtClientUtils.showLsfViewError(host, "'" + canonicalName + "' belongs to another window");
            GwtClientUtils.logLsfViewError("'" + canonicalName + "' is drawn in window '" + windowName(element) + "', so this window has no button to place");
            return;
        }

        Element asked = hosts.get(canonicalName);
        if (asked != null && asked != host) { // the first host keeps it, so a second cannot be left silently empty
            GwtClientUtils.showLsfViewError(host, "'" + canonicalName + "' is already placed by another <Lsf>");
            GwtClientUtils.logLsfViewError("'" + canonicalName + "' is placed by more than one <Lsf>; the first one keeps it");
            return;
        }

        hosts.put(canonicalName, host);
        fill(canonicalName, host);
    }

    private static String windowName(GNavigatorElement element) {
        return element.getDrawWindow() != null ? element.getDrawWindow().canonicalName : "none";
    }

    // the template form: the button takes the place of the <Lsf:name> element, which is consumed. Every name here
    // comes from the bucket the window just drew, so there is nothing to report
    public void replacePlace(String canonicalName, Element root) {
        Element place = GwtClientUtils.getLsfPlace(root, canonicalName);
        if (place == null) // this window's template gives the element no place, so it is not drawn
            return;

        filled.put(canonicalName, place.getParentElement());
        place.getParentElement().replaceChild(buttons.get(canonicalName).getElement(), place);
    }

    private void fill(String canonicalName, Element host) {
        NavigatorImageButton button = buttons.get(canonicalName);
        if (button == null || filled.get(canonicalName) != null) // nothing to fill it with yet, or already filled
            return;

        filled.put(canonicalName, host);
        host.appendChild(button.getElement());
    }

    // whatever the window draws now goes into the host that has been waiting for it
    private void fillAsked() {
        hosts.foreachEntry(this::fill);
    }

    public void unmountButton(String canonicalName, Element host) {
        if (hosts.get(canonicalName) == host) // the component took the host away, so nothing waits for it any more
            hosts.remove(canonicalName);

        if (filled.get(canonicalName) != host) {
            GwtClientUtils.clearLsfViewError(host); // a host that was never given a button may be holding the diagnostic instead
            return;
        }

        filled.remove(canonicalName);
        NavigatorImageButton button = buttons.get(canonicalName);
        if (button != null)
            park.appendChild(button.getElement()); // back to the park, still attached, tooltip alive
    }

    // everything goes back to the park, so the next render starts from a clean panel without losing a single button
    public void parkAll() {
        buttons.foreachEntry((name, button) -> {
            if (filled.get(name) != null) {
                filled.remove(name);
                park.appendChild(button.getElement());
            }
        });
    }

    // the names of the current bucket, in the order the standard panel draws them
    public List<String> getDrawnNames() {
        return drawnNames;
    }
}
