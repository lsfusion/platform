package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// WINDOW ... CUSTOM '<nav><Lsf:orders></nav>': the window's markup is the application's, and the platform's own
// buttons are put into the <Lsf:name> places in it - the same template shape a DESIGN container uses. An element the
// template gives no place is not drawn.
public class CustomNavigatorView extends ParkedNavigatorView {

    // what the panel currently holds; null until it has been rendered at all. The template is one of the two, since a
    // CUSTOM property recomputes it
    private String renderedCustom;
    private List<String> renderedNames;

    public CustomNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);

        GwtClientUtils.addClassName(panel, "panel-custom");
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected) {
        buttons.refresh(elements, selected);

        // caption, image and selection reach the DOM through the button itself, so the template is rewritten only when
        // the elements to place change - the application's own markup, and whatever state it holds, then survives a
        // selection change. Placing a button CONSUMES its place, so the places only come back by re-rendering
        List<String> names = buttons.getDrawnNames();
        if (window.custom.equals(renderedCustom) && names.equals(renderedNames))
            return;
        renderedCustom = window.custom;
        renderedNames = new ArrayList<>(names); // getDrawnNames hands out the list the next refresh clears

        buttons.parkAll();
        GwtClientUtils.setLsfTemplate(panel.getElement(), window.custom);

        for (String canonicalName : buttons.getDrawnNames())
            buttons.replacePlace(canonicalName, panel.getElement());
    }

}
