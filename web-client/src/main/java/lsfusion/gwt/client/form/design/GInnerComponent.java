package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.view.GFlexAlignment;

public class GInnerComponent extends GComponent {
    public String caption;

    public GInnerComponent() {
    }

    public GInnerComponent(String caption) {
        this.caption = caption;
        alignment = GFlexAlignment.STRETCH;
    }
}