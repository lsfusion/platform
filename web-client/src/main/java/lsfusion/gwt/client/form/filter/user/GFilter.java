package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class GFilter extends GComponent {
    public GPropertyDraw property;
    
    public GFilter() {
    }

    public GFilter(GPropertyDraw property){
        this.property = property;
        alignment = GFlexAlignment.CENTER;
    }
}
