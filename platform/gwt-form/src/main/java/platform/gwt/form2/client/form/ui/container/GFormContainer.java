package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

public class GFormContainer extends GAbstractFormContainer {
    private Panel panel;

    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.gwtVertical) {
            panel = new VerticalPanel();
            panel.addStyleName("gwtVertical");
        } else {
            panel = new HorizontalPanel();
            panel.addStyleName("getHorizontal");
        }
    }

    @Override
    public Panel getUndecoratedView() {
        return panel;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        if (key.gwtIsLayout) {
            panel.add(childView);
        } else {
            panel.add(childView);
        }
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        panel.remove(childView);
    }

    @Override
    protected boolean containerHasChild(Widget childView) {
        for (Widget child : panel) {
            if (child == childView) {
                return true;
            }
        }
        return false;
    }
}
