package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.PlacedViews;
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
    // what is wrong with a name, if anything. `requireLsf` is the difference between the two surfaces: a component's
    // <Lsf> may only place an element declared LSF - anything else it is expected to draw from the projection - while
    // a template places the window's ordinary buttons, and LSF is refused outside a React window in the first place
    // (CheckNavigatorElementsTask), so asking for it there would call every element of every template a mistake
    private PlacedViews.Problem problem(String canonicalName, boolean requireLsf) {
        GNavigatorElement element = navigatorController.getElement(canonicalName);
        if (element == null) // a name no element has will never be drawn
            return new PlacedViews.Problem("'" + canonicalName + "' is not a navigator element",
                    "'" + canonicalName + "' is not a navigator element, so nothing will ever be placed here");
        if (requireLsf && !element.lsf) // declared, but the component owns it: there is no drawing to place
            return new PlacedViews.Problem("'" + canonicalName + "' has no LSF",
                    "'" + canonicalName + "' has no LSF, so the component draws it from the projection instead of placing it");
        // whether the window draws it RIGHT NOW depends on the selection and is nobody's mistake, but which window
        // draws it at all does not - waiting for a button another window owns would wait forever, in silence
        if (element.getDrawWindow() != window)
            return new PlacedViews.Problem("'" + canonicalName + "' belongs to another window",
                    "'" + canonicalName + "' is drawn in window '" + windowName(element) + "', so this window has no button to place");
        return null;
    }

    private final PlacedViews placed = new PlacedViews(new PlacedViews.Views() {
        @Override
        public PlacedViews.Problem problem(String canonicalName) {
            return NavigatorButtons.this.problem(canonicalName, true);
        }

        @Override
        public boolean place(String canonicalName, Element host) {
            NavigatorImageButton button = buttons.get(canonicalName);
            if (button == null) // the window does not draw it yet; the host waits
                return false;

            host.appendChild(button.getElement());
            return true;
        }

        @Override
        public void park(String canonicalName) {
            NavigatorImageButton button = buttons.get(canonicalName);
            if (button != null)
                park.appendChild(button.getElement()); // back to the park, still attached, tooltip alive
        }
    });
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
        placed.retryPending();
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
            placed.viewRemoved(name, true); // the host waits: the selection may bring this element back
            panel.remove(buttons.remove(name));
        }
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

        // claimed BEFORE the DOM moves: no host and no cleanup here, so a second place for one name would silently
        // steal the button from the first
        if (placed.consumePlace(canonicalName))
            place.getParentElement().replaceChild(buttons.get(canonicalName).getElement(), place);
    }

    // a place the template wrote and nothing filled. A name this window simply does not draw RIGHT NOW is not a
    // mistake and stays silent; a name that is not an element, has no LSF or belongs to another window is the same
    // mistake a component's <Lsf> makes, and says so in the place itself - the only path that used to say nothing
    public void reportPlace(String canonicalName, Element root) {
        Element place = GwtClientUtils.getLsfPlace(root, canonicalName);
        if (place == null) // the place was filled, so it is gone from the document
            return;

        PlacedViews.Problem problem = problem(canonicalName, false);
        if (problem == null) // the element is this window's and simply not drawn right now, which is not a mistake
            return;

        GwtClientUtils.showLsfViewError(place, problem.shown);
        GwtClientUtils.logLsfViewError(problem.logged);
    }

    // everything goes back to the park, so the next render starts from a clean panel without losing a single button
    public void parkAll() {
        placed.parkAll();
    }

    // the component's half of the crossing: it renders the node, we fill it
    public void mountButton(String canonicalName, Element host) {
        placed.mount(canonicalName, host);
    }

    public void unmountButton(String canonicalName, Element host) {
        placed.unmount(canonicalName, host);
    }

    // the names of the current bucket, in the order the standard panel draws them
    public List<String> getDrawnNames() {
        return drawnNames;
    }
}
