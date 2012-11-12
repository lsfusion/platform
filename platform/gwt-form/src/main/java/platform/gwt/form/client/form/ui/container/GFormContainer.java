package platform.gwt.form.client.form.ui.container;

import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form.shared.view.GComponent;
import platform.gwt.form.shared.view.GContainer;

public class GFormContainer extends GAbstractFormContainer {
    private CellPanel panel;

    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.drawVertical()) {
            panel = new VerticalPanel();
            panel.addStyleName("gwtVertical");
        } else {
            panel = new HorizontalPanel();
            panel.addStyleName("gwtHorizontal");
        }
    }

    @Override
    public Widget getUndecoratedView() {
        return panel;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        if (position == -1 || position >= childrenViews.size() - 1) {
            panel.add(childView);
        } else {
            panel.clear();
            for (Widget childComponent : childrenViews.values()) {
                panel.add(childComponent);
            }
        }

        if (childKey.hAlign.equals(GContainer.Alignment.RIGHT)) {
            panel.setCellHorizontalAlignment(childView, HasHorizontalAlignment.HorizontalAlignmentConstant.endOf(HasDirection.Direction.LTR));
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

    @Override
    public void setTableCellSize(Widget child, String size, boolean width) {
        if (size != null) {
            if (width) {
                // на данный момент есть баг GWT + IE:
                // http://code.google.com/p/google-web-toolkit/issues/detail?id=2065
                // поэтому приходится делать так, хотя предполагается использовать panel.setCellWidth()
                child.getElement().getParentElement().getStyle().setProperty("width", size);
            } else {
                child.getElement().getParentElement().getStyle().setProperty("height", size);
            }
        }
    }

    @Override
    public void setChildSize(GComponent child, String width, String height) {
        Widget childView = childrenViews.get(child);
        if (childView != null) {
            if (width != null) {
                setTableCellSize(childView, width, true);
                if (child.fillHorizontal <= 0) {
                    childView.setWidth(width);
                }
            }
            if (height != null) {
                setTableCellSize(childView, height, false);
                if (child.fillVertical <= 0) {
                    childView.setHeight(height);
                }
            }
        }
    }

    @Override
    public void addDirectly(Widget child) {
        panel.add(child);
    }
}
