package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.client.form.ui.GSplitPane;
import platform.gwt.form2.shared.view.GComponent;
import platform.gwt.form2.shared.view.GContainer;
import platform.gwt.form2.shared.view.GContainerType;

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

    public void update() {
        splitPane.update();
    }

    @Override
    public void setChildSize(GComponent child, String width, String height) {
        Widget childView = childrenViews.get(child);
        if (childView != null) {
            splitPane.setWidgetSize(childView, width, height);
        }
    }
}
