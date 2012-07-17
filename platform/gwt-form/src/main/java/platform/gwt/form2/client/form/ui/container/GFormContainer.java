package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

public class GFormContainer extends GAbstractFormContainer {
    private Panel panel;

    public static long nextid = 0;
    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.title != null) {
            //todo:
//            CaptionLayoutPanel
//            containerComponent = new Ca
        }

        if (key.gwtVertical) {
            panel = new VerticalPanel();
            panel.addStyleName("vlayout");
        } else {
            panel = new HorizontalPanel();
            panel.addStyleName("hlayout");
        }

        panel.getElement().setAttribute("layoutid", "" + nextid++);
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
