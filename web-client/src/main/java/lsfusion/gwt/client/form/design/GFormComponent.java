package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.view.GFlexAlignment;

public class GFormComponent extends GComponent {
    public String caption;

    public GFormComponent() {
    }

    public GFormComponent(String caption) {
        this.caption = caption;
        alignment = GFlexAlignment.STRETCH;
    }
}