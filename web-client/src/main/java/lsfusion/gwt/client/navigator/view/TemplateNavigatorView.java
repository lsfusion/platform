package lsfusion.gwt.client.navigator.view;

import com.google.gwt.core.client.JsArrayString;
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
public class TemplateNavigatorView extends ParkedNavigatorView {

    // what the panel currently holds; null until it has been rendered at all. The template is one of the two, since a
    // CUSTOM property recomputes it
    private String renderedCustom;
    private List<String> renderedNames;

    public TemplateNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
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

        GwtClientUtils.reportComponentName(window.custom, window.canonicalName);

        buttons.parkAll();
        GwtClientUtils.setLsfTemplate(panel.getElement(), window.custom);

        for (String canonicalName : buttons.getDrawnNames())
            buttons.replacePlace(canonicalName, panel.getElement());

        // and whatever the template asked for that nothing filled. A literal is read when the module is read and a
        // name that is not an element is refused there; a template a property computes is read by nobody, so this is
        // where its mistakes are found - which is the path where a typo lives longest
        JsArrayString placeNames = GwtClientUtils.getLsfPlaceNames(window.custom);
        for (int i = 0; i < placeNames.length(); i++)
            buttons.reportPlace(placeNames.get(i), panel.getElement());
    }
}
