package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;

public class SizedWidget {
    public final Widget widget;
    public final GSize width;
    public final GSize height;

    public SizedWidget(Widget widget) {
        this(widget, null, null);
    }

    public SizedWidget(Widget widget, GSize width, GSize height) {
        this.widget = widget;
        this.width = width;
        this.height = height;
    }

    private void add(SizedFlexPanel panel, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, panel.getWidgetCount(), alignment, flex, shrink);
    }

    private void add(SizedFlexPanel panel, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink) {
        boolean vertical = panel.isVertical();
        panel.addSized(widget, beforeIndex, flex, shrink, vertical ? height : width, alignment, alignment == GFlexAlignment.STRETCH, vertical ? width : height);
    }

    public void addFill(SizedFlexPanel panel) {
        add(panel, GFlexAlignment.STRETCH, 1, true);
    }

    public void addFill(SizedFlexPanel panel, int beforeIndex) {
        add(panel, beforeIndex, GFlexAlignment.STRETCH, 1, true);
    }

    public void add(SizedFlexPanel panel, GFlexAlignment alignment) {
        add(panel, alignment, 0, false);
    }
}
