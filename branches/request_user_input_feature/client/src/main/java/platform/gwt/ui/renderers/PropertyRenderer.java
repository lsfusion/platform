package platform.gwt.ui.renderers;

import com.smartgwt.client.widgets.Canvas;

public interface PropertyRenderer {
    public Canvas getComponent();
    public void setValue(Object value);
}
