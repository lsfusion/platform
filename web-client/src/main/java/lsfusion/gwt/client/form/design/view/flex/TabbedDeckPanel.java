package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;

public class TabbedDeckPanel extends FlexPanel {
    private Widget visibleWidget;

    public TabbedDeckPanel() {
        super(true);
    }

    @Override
    public boolean remove(Widget w) {
        int ind = getWidgetIndex(w);
        if (ind != -1) {
            if (visibleWidget == w) {
                visibleWidget = null;
            }
            return super.remove(w);
        }

        return false;
    }

    public void showWidget(int index) {
        checkIndexBoundsForAccess(index);

        Widget oldWidget = visibleWidget;
        visibleWidget = getWidget(index);

        if (visibleWidget != oldWidget) {
            visibleWidget.setVisible(true);
            if (oldWidget != null) {
                oldWidget.setVisible(false);
            }
        }
    }

    @Override
    public void onResize() {
        if (visibleWidget instanceof RequiresResize) {
            ((RequiresResize) visibleWidget).onResize();
        }
    }
}
