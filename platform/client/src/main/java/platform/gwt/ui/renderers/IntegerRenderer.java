package platform.gwt.ui.renderers;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;

public class IntegerRenderer extends IButton implements PropertyRenderer {
    public IntegerRenderer() {
        super();
    }

    public Canvas getComponent() {
        return this;
    }

    @Override
    public void setValue(Object value) {
        if (value == null) {
            return;
        }

//        if (value instanceof Integer) {
            setTitle(value.toString());
//        }
    }
}
