package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.SizedWidget;

public class ComponentWidget {

    public CaptionWidget caption;

    public SizedWidget widget;

    public ComponentWidget(SizedWidget widget, CaptionWidget caption) {
        this.caption = caption;
        this.widget = widget;
    }

    public ComponentWidget(Widget widget) {
        this(new SizedWidget(widget), null);
    }

    public Widget getWidget() {
        return widget.widget;
    }
}
