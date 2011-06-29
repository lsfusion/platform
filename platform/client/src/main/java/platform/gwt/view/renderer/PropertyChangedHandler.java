package platform.gwt.view.renderer;

import platform.gwt.view.GPropertyDraw;

public interface PropertyChangedHandler {
    void onChanged(GPropertyDraw property, Object value);
}
