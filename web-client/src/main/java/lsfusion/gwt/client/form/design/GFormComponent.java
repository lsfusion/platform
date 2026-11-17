package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.view.GFlexAlignment;

// the component a form shown IN a container of another form (SHOW ... IN or DIALOG ... IN) is drawn in. The client
// makes it, not the server, so it sets the alignment itself: the container's layout fails on a component with none
public class GFormComponent extends GComponent {

    public GFormComponent() {
        alignment = GFlexAlignment.STRETCH;
    }
}
