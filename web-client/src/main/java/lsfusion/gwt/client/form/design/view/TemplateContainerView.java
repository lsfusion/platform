package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

// CUSTOM 'html': the container's children are placed into the <Lsf:sID> places of an HTML template.
// A 'simple' custom container (custom = '') has no template and lays its children out in order, like a plain panel.
public class TemplateContainerView extends ParkedContainerView {

    private String renderedCustom; // null until the template has been rendered; `` is a legitimate custom value, so it cannot mark that

    private final boolean simpleCustom;

    public TemplateContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        simpleCustom = "".equals(container.getCustom());
        GwtClientUtils.addClassName(panel, "panel-custom");
    }

    @Override
    protected void addImpl(int index) {
        if (simpleCustom)
            getChildView(index).add(panel, getChildPosition(index));
        else {
            super.addImpl(index); // park the child; the template is (re)rendered on the next layout, which places it
            invalidateTemplate();
        }
    }

    @Override
    protected void removeImpl(int index) {
        if (simpleCustom)
            getChildView(index).remove(panel, getChildPosition(index));
        else {
            super.removeImpl(index);
            invalidateTemplate(); // placing a child consumed its place, so re-render to bring the place back
        }
    }

    // force updateLayout to render the template again — the only place that parks all children, rewrites the HTML and
    // re-places them, so both adding and removing a child go through the same path
    private void invalidateTemplate() {
        renderedCustom = null;
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        if (!simpleCustom && !container.getCustom().equals(renderedCustom)) {
            renderedCustom = container.getCustom();

            // setInnerHTML destroys the elements of the children the previous template held, so they are parked first:
            // a child the new template gives no place then stays alive (parked, not shown) instead of detached
            for (int i = 0, size = children.size(); i < size; i++)
                parkChild(i);

            GwtClientUtils.reportComponentName(renderedCustom, container.sID);

            GwtClientUtils.setLsfTemplate(panel.getElement(), renderedCustom);

            for (int i = 0, size = children.size(); i < size; i++)
                placeChild(i);
            resizeChildren();

            reportUnplacedPlaces();
        }
        super.updateLayout(requestIndex, childrenVisible);
    }

    public void updateCustom(String custom) {
        if (custom == null) // "nothing computed yet", not "no template": the container keeps the markup it has - the
            return;         // literal that was written beside the property, or the placeholder standing in for one,
                            // which is what the window's computed template does too (GCustomWindowNavigator)
        this.container.setCustom(custom);
    }

    // moves the child's element into its <Lsf:sID> place; a child the template gives no place stays parked, not shown
    private void placeChild(int index) {
        getChildView(index).replace(panel, children.get(index).sID);
    }

    // a place naming nothing this container has. A literal template is read when the form is built and such a name is
    // refused there; a template a property computes is read by nobody, so this is where it is found - and it used to
    // be found nowhere, leaving a blank spot and an inert <lsf:name> in the document.
    // A place whose child is simply not there right now is NOT this: a child a SHOWIF dropped keeps its component, so
    // it still names one and stays silent, exactly as it does for a component's <Lsf>
    private void reportUnplacedPlaces() {
        JsArrayString placeNames = GwtClientUtils.getLsfPlaceNames(container.getCustom());
        for (int i = 0; i < placeNames.length(); i++) {
            String name = placeNames.get(i);
            if (formHas(name)) // anything the form has, wherever it now sits
                continue;

            Element place = GwtClientUtils.getLsfPlace(panel.getElement(), name);
            if (place != null) { // a place that WAS filled is gone from the document, so only an empty one is left
                GwtClientUtils.showLsfViewError(place, "'" + name + "' is not a child of '" + container.sID + "'");
                GwtClientUtils.logLsfViewError("component '" + name + "' is not a child of container '" + container.sID
                        + "', so nothing will ever be placed there");
            }
        }
    }

    // what a place may name, which is the server's own rule when it reads a literal template (FormView.names): a
    // child's design identifier, or one of its parts - a caption or a comment drawn inline.
    // Over the DECLARED children and not the built ones: a child whose SHOWIF was false when the form was built has no
    // view yet and would otherwise have its place called a typo - the same list the React path asks (findDeclared)
    // asked of the whole FORM and not of this container alone. A child this container no longer holds - an extending
    // module may move one out - leaves its place unfilled, and the server leaves such a place alone for exactly that
    // reason, so the client must not call it a mistake either: the two ends would then disagree about a form the
    // server accepted. Only a name the form has nowhere is one nothing will ever fill
    private boolean formHas(String name) {
        return names(formController.getForm().mainContainer, name);
    }

    private boolean names(GContainer where, String name) {
        for (GComponent child : where.children) {
            if (namesOrPart(name, child.sID))
                return true;

            if (child instanceof GContainer && names((GContainer) child, name))
                return true;
        }
        return false;
    }

    private static boolean namesOrPart(String name, String base) {
        return name.equals(base) || name.startsWith(base + ".");
    }

}
