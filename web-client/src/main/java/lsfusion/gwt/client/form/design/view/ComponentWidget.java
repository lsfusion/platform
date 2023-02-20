package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.SizedWidget;

public class ComponentWidget {

    public CaptionWidget caption;

    public SizedWidget widget;

    public ComponentWidget(Widget widget, Widget caption) {
        this(new SizedWidget(widget), caption != null ? new CaptionWidget(caption) : null);
    }

    public ComponentWidget(SizedWidget widget, CaptionWidget caption) {
        this.caption = caption;
        this.widget = widget;
    }

    public ComponentWidget(Widget widget) {
        this(widget, null);
    }

    public Widget getWidget() {
        return widget.widget;
    }
}
