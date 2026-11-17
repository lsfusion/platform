package lsfusion.server.logics.navigator;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.physics.dev.id.name.DuplicateElementsChecker.DuplicateElementsFound;

import java.util.*;
import java.util.function.Function;

// The navigator : its elements, a tree under the root, and the windows they are drawn in. The logics has one, and every
// module writes what it declares straight into it and finds in it what its code names, as a module does with a form it
// extends (FormEntity) - a module keeps nothing of the navigator's itself. The code of a customization changes a copy
// of it, which is what a client customized so draws - as a form is opened from its customized copy
public class NavigatorEntity {

    private final NavigatorElement root;
    // every element, in a folder or in none; and the windows. Both in the order they are declared - the modules' order,
    // then the order in a module : a client searches the windows that hold forms in the order it is sent them
    private final NFOrderSet<NavigatorElement> elements = NFFact.orderSet();
    private final NFOrderSet<AbstractWindow> windows = NFFact.orderSet();

    // what the elements and the windows of the logics' navigator are in this one, a copy - kept as a form keeps the
    // mapping of each form it is copied of (FormEntity.getExEntity); none for the logics' own
    private final NavigatorMapping mapping;

    // the logics' : the modules add the root and the rest as they are read
    public NavigatorEntity(NavigatorElement root) {
        this.root = root;
        this.mapping = null;
    }

    // every element is copied, not only what the root leads to : a statement can name an element that is in no folder,
    // to put it into one. And every window, the ones no element is drawn in as well : a statement can hide any, and
    // turn over how one that holds forms draws them
    public NavigatorEntity(NavigatorEntity src, Version version) {
        mapping = new NavigatorMapping(version);
        mapping.add(windows, src.windows);
        mapping.add(elements, src.elements);
        this.root = mapping.get(src.root);
    }

    // an element or a window of the logics' navigator as it is in this one : itself, or its copy - every one is copied by
    // the time the copy is final, so only one that is there is asked for
    public <M extends NavigatorMappingInterface<? super M>> M getEx(M object) {
        return mapping != null ? mapping.getFinal(object) : object;
    }

    public NavigatorElement getRoot() {
        return root;
    }

    // a module declares an element, a window, at its version
    public void addElement(NavigatorElement element, Version version) {
        elements.add(element, version);
    }

    public void addWindow(AbstractWindow window, Version version) {
        windows.add(window, version);
    }

    // what a module that reads at the version sees - what it declared, and what the modules it requires did -, found by
    // the canonical name, as an object of a form is (FormEntity.getNFObject) : a name in its code is looked up so in every
    // namespace the code can mean (ModuleNavigatorFinder). The navigator is read so once it is final as well - by a
    // script, which finds a window in it
    public NavigatorElement getNFElement(String canonicalName, Version version) {
        for (NavigatorElement element : elements.getNFIt(version, true))
            if (element.getCanonicalName().equals(canonicalName))
                return element;
        return null;
    }

    public AbstractWindow getNFWindow(String canonicalName, Version version) {
        for (AbstractWindow window : windows.getNFIt(version, true))
            if (window.getCanonicalName().equals(canonicalName))
                return window;
        return null;
    }

    // what there is in the navigator once every module that writes into it is read
    public ImSet<NavigatorElement> getElements() {
        return elements.getSet();
    }

    // a client names the element it runs by the canonical name
    @IdentityLazy
    public NavigatorElement getElement(String canonicalName) {
        for (NavigatorElement element : getElements())
            if (element.getCanonicalName().equals(canonicalName))
                return element;
        return null;
    }

    public ImOrderSet<AbstractWindow> getWindows() {
        return windows.getOrderSet();
    }

    // the logics' navigator is finalized and checked once every module is read, in two tasks of the start
    // (FinalizeNavigatorElementsTask, CheckNavigatorElementsTask); a copy - both at once, once its code is compiled
    // (as a form an evaluated code declares, FormEntity.finalizeAndPreread) : final, a copy is as closed to a later change
    // as the logics' navigator is
    public void finalizeAndCheck(BaseLogicsModule LM) {
        finalizeAroundInit(LM);
        check();
    }

    public void finalizeAroundInit(BaseLogicsModule LM) {
        for (NavigatorElement element : getElements())
            element.finalizeAroundInit(LM);
    }

    public void check() {
        checkDuplicateNames();

        ImOrderSet<AbstractWindow> windows = getWindows();
        for (AbstractWindow window : windows)
            window.checkCustoms();

        // a template is read once per window and not once per element : it can be as long as an application's markup
        Map<NavigatorWindow, Set<String>> templatePlaces = new HashMap<>();
        for (NavigatorElement element : getElements())
            element.check(windows, templatePlaces);
    }

    // two elements, two windows of one canonical name - which a client names them by -, each declared by a module that
    // does not see the other (an element is told by its place in the code, a window by its caption)
    private void checkDuplicateNames() {
        String errText = checkDuplicateNames("navigator element", getElements(), NavigatorElement::getCanonicalName, NavigatorElement::toString)
                + checkDuplicateNames("window", getWindows(), AbstractWindow::getCanonicalName, window -> window.caption.getSourceString());

        if (!errText.isEmpty())
            throw new DuplicateElementsFound(errText);
    }

    private static <T> String checkDuplicateNames(String textName, Iterable<T> elements, Function<T, String> getCanonicalName, Function<T, String> describe) {
        Map<String, List<T>> canonicalNameToElement = new HashMap<>();
        for (T element : elements)
            canonicalNameToElement.computeIfAbsent(getCanonicalName.apply(element), cn -> new ArrayList<>()).add(element);

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<T>> entry : canonicalNameToElement.entrySet()) {
            if (entry.getValue().size() > 1) {
                builder.append("\n\t").append("Ambiguous ").append(textName).append(" canonical name '").append(entry.getKey()).append("': ");
                for (T element : entry.getValue())
                    builder.append("\n\t\t").append(describe.apply(element));
            }
        }
        return builder.toString();
    }
}
