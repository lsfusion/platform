package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

public class GFormSplitPane extends GAbstractFormContainer {
    private SplitLayoutPanel splitPane;

    public GFormSplitPane(GContainer key) {
        this.key = key;

        splitPane = new SplitLayoutPanel();
    }

    @Override
    public Panel getUndecoratedView() {
        return splitPane;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        if (key.gwtVertical) {
            if (position == 0) {
                splitPane.addNorth(childView, 250);
            } else {
                splitPane.add(childView);
            }
        } else {
            if (position == 0) {
                splitPane.addWest(childView, 250);
            } else {
                splitPane.add(childView);
            }
        }
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        splitPane.remove(childView);
    }

    @Override
    protected boolean containerHasChild(Widget childView) {
        return splitPane.getWidgetIndex(childView) != -1;
    }
}
