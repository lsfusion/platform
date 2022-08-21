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

    public void add(SizedFlexPanel panel, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, panel.getWidgetCount(), alignment, flex, shrink);
    }

    public void add(SizedFlexPanel panel, int beforeIndex, double flex, boolean shrink, GFlexAlignment alignment, boolean alignShrink) {
        boolean vertical = panel.isVertical();
        panel.addSized(widget, beforeIndex, flex, shrink, vertical ? height : width, alignment, alignShrink, vertical ? width : height);
    }

    private void add(SizedFlexPanel panel, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, beforeIndex, flex, shrink, alignment, alignment == GFlexAlignment.STRETCH);
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

    public void add(SizedFlexPanel panel, int beforeIndex, GFlexAlignment alignment) {
        add(panel, beforeIndex, alignment, 0, false);
    }

    public SizedWidget override(Widget view) {
        return new SizedWidget(view, width, height);
    }

    public SizedWidget override(GSize width, GSize height) {
        return new SizedWidget(widget, width != null ? width : this.width, height != null ? height : this.height);
    }
}
