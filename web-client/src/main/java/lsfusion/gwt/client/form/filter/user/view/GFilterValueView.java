package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;

public class GFilterValueView extends ResizableSimplePanel {
    public void focusOnValue() {
    }

    public void propertyChanged(GPropertyFilter condition) {}

    public void startEditing(Event keyEvent) {
        focusOnValue();
    }
}
