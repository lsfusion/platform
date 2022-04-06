package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;

public class SizedWidget {
    public final Widget widget;
    public final Integer width;
    public final Integer height;

    public SizedWidget(Widget widget) {
        this(widget, null, null);
    }

    public SizedWidget(Widget widget, Integer width, Integer height) {
        this.widget = widget;
        this.width = width;
        this.height = height;
    }

    private void add(FlexPanel panel, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, panel.getWidgetCount(), alignment, flex, shrink);
    }

    private void add(FlexPanel panel, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink) {
        boolean vertical = panel.isVertical();

        panel.setOppositeSize(widget, vertical ? width : height, alignment);

        panel.add(widget, beforeIndex, alignment, flex, shrink, vertical ? height : width);
    }

    public void addFill(FlexPanel panel) {
        add(panel, GFlexAlignment.STRETCH, 1, true);
    }

    public void addFill(FlexPanel panel, int beforeIndex) {
        add(panel, beforeIndex, GFlexAlignment.STRETCH, 1, true);
    }

    public void add(FlexPanel panel, GFlexAlignment alignment) {
        add(panel, alignment, 0, false);
    }
}
