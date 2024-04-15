package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class PopupOwner {

    public static final PopupOwner GLOBAL = new PopupOwner(null, null);

    public final Widget widget;

    public final Element element;

    public PopupOwner(Widget widget) {
        this(widget, widget.getElement());
    }
    public PopupOwner(Widget widget, Element element) {
        this.widget = widget;
        this.element = element;
    }
}
