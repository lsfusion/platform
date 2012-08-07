package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.ui.*;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

public class GFormContainer extends GAbstractFormContainer {
    private CellPanel panel;

    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.isVertical) {
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
        panel.add(childView);

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
        if (width) {
            panel.setCellWidth(child, size);
        } else {
            panel.setCellHeight(child, size);
        }
    }
}
