package platform.gwt.view.renderer;

import com.smartgwt.client.widgets.Canvas;
import platform.gwt.view.GPropertyDraw;

import java.io.Serializable;

public interface GPropertyRenderer extends Serializable {
    Canvas getComponent();
    void setValue(GPropertyDraw property, Object value);
}
