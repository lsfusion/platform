package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.*;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

public class GFormSplitPane extends GAbstractFormContainer {
    private GFormSplitPanel splitPane;

    public GFormSplitPane(GContainer key) {
        this.key = key;

        splitPane = new GFormSplitPanel();
    }

    @Override
    public Widget getUndecoratedView() {
        return splitPane;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        if (key.isVertical) {
            if (position == 0) {
                splitPane.addNorth(childView, 250);
            } else if (splitPane.getCenter() != null) {
                Widget center = splitPane.getCenter();
                splitPane.remove(center);
                splitPane.addNorth(center, 250);
                splitPane.add(childView);
            } else {
                splitPane.add(childView);
            }
        } else {
            if (position == 0) {
                splitPane.addWest(childView, 250);
            } else if (splitPane.getCenter() != null) {
                Widget center = splitPane.getCenter();
                splitPane.remove(center);
                splitPane.addWest(center, 250);
                splitPane.add(childView);
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

    public void setSplittersInitialPosition() {
        int index = 0;
        double previousSizes = 0;
        for (GComponent  child : childrenViews.keySet()) {
            if (index < childrenViews.size() - 1) {
                double size = 0;
                if (!key.isVertical && child.absoluteWidth != -1) {
                    size = child.absoluteWidth;
                } else if (key.isVertical && child.absoluteHeight != -1) {
                    size = child.absoluteHeight;
                }
                if (size == 0) {
                    size = (key.isVertical ? child.fillVertical : child.fillHorizontal) / getTotalFill(!key.isVertical) *
                            (key.isVertical ? splitPane.getOffsetHeight() : splitPane.getOffsetWidth());
                }

                splitPane.setWidgetSize(childrenViews.get(child), previousSizes + size);
                previousSizes += size;
            }
            index++;
        }
    }

    private double getTotalFill(boolean width) {
        double sum = 0;
        for (GComponent child : childrenViews.keySet()) {
            sum += width ? child.fillHorizontal : child.fillVertical;
        }
        return sum;
    }

    class GFormSplitPanel extends SplitLayoutPanel {
        public Widget getCenter() {
            return super.getCenter();
        }
    }
}
