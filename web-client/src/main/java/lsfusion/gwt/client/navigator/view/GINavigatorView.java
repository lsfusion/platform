package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.GNavigatorElement;

import java.util.LinkedHashSet;

public interface GINavigatorView {

    // the same widget for the window's lifetime: the window layout writes layout data onto that exact widget, and
    // WindowsController.updateElementClass adds and removes classes by traversing from it
    Widget getView();

    // the only place where server-pushed caption / image / elementClass / hide reach the DOM, since on desktop there
    // is no per-element notification, just one global update - which is reentrant, so refresh must not call it back
    void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected);

    void onParentSelected();

    void onSelected();
}
