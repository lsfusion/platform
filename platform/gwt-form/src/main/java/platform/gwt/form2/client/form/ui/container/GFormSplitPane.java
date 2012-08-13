package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.form.ui.GSplitPane;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;
import platform.gwt.view2.GContainerType;

public class GFormSplitPane extends GAbstractFormContainer {
    private GSplitPane splitPane;

    public GFormSplitPane(GContainer key) {
        this.key = key;

        splitPane = new GSplitPane(key.type == GContainerType.VERTICAL_SPLIT_PANEL);
    }

    @Override
    public Widget getUndecoratedView() {
        return splitPane.getComponent();
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        splitPane.addWidget(childView);
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        splitPane.remove(childView);
    }

    @Override
    protected boolean containerHasChild(Widget childView) {
        return splitPane.hasChild(childView);
    }

    public void setWidgetSize(Widget widget, String size, boolean width) {
        splitPane.setWidgetSize(widget, size, width);
    }

    public void update() {
        splitPane.update();
    }
}
