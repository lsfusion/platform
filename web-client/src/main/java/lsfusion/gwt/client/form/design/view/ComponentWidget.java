package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedWidget;

public class ComponentWidget {

    public CaptionWidget caption;

    public ComponentViewWidget widget;

    public ComponentWidget(Widget widget, Widget caption) {
        this(widget, caption != null ? new CaptionWidget(caption, GFlexAlignment.START, GFlexAlignment.CENTER) : null);
    }

    public ComponentWidget(Widget widget, CaptionWidget caption) {
        this(new SizedWidget(widget).view, caption);
    }

    public ComponentWidget(ComponentViewWidget widget, CaptionWidget caption) {
        this.caption = caption;
        this.widget = widget;
    }

    public ComponentWidget(Widget widget) {
        this(widget, (CaptionWidget) null);
    }
}
