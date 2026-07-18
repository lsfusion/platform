package lsfusion.gwt.client.form.design.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;

// CUSTOM 'html': the container's children are placed into the [sID] slots of an HTML template.
// A 'simple' custom container (custom = '') has no template and lays its children out in order, like a plain panel.
public class CustomContainerView extends ParkedContainerView {

    private String renderedCustom; // null until the template has been rendered; `` is a legitimate custom value, so it cannot mark that

    private final boolean simpleCustom;

    public CustomContainerView(GFormController formController, GContainer container) {
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
            invalidateTemplate(); // placing a child consumed its [sID] slot, so re-render to bring the slot back
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
            // a child whose slot the new template lacks then stays alive (parked, not shown) instead of detached
            for (int i = 0, size = children.size(); i < size; i++)
                parkChild(i);

            panel.getElement().setInnerHTML(getTagCustom(renderedCustom));

            for (int i = 0, size = children.size(); i < size; i++)
                placeChild(i);
            resizeChildren();
        }
        super.updateLayout(requestIndex, childrenVisible);
    }

    public void updateCustom(String custom) {
        this.container.setCustom(custom);
    }

    // moves the child's element into its [sID] slot; a child the template has no slot for stays parked, i.e. not shown
    private void placeChild(int index) {
        getChildView(index).replace(panel, children.get(index).sID);
    }

    private String getTagCustom(String rawCustom) {
        while (true) {
            int openBracket = rawCustom.indexOf("[");
            int closeBracket = rawCustom.indexOf("]");
            if (openBracket == -1 || closeBracket == -1) {
                break;
            } else {
                String tagName = rawCustom.substring(openBracket + 1, closeBracket);
                rawCustom = rawCustom.replace("[" + tagName + "]", "<" + tagName + "></" + tagName + ">");
            }
        }

        return rawCustom;
    }
}
