package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ButtonBase;
import lsfusion.gwt.client.base.GwtClientUtils;

public class FormButton extends ButtonBase {
    public FormButton(Element element) {
        super(element == null ? Document.get().createPushButtonElement() : element);
        if (element == null)
           GwtClientUtils.addClassName(this, "btn");
    }

    public FormButton() {
        this((Element)null);
    }

    public FormButton(String html) {
        this();
        setHTML(html);
    }
    
    public FormButton(String html, ButtonStyle style) {
        this(html);
        setStyle(style);
    }

    public FormButton(String html, ClickHandler clickHandler) {
        this(html, null, clickHandler);
    }
    
    public FormButton(String html, ButtonStyle style, ClickHandler clickHandler) {
        this(html, style);
        addClickHandler(clickHandler);
    }

    public void setStyle(ButtonStyle style) {
        if (style != null) {
            switch (style) {
                case PRIMARY:
                   GwtClientUtils.addClassName(this, "btn-primary");
                    break;
                case SECONDARY:
                   GwtClientUtils.addClassName(this, "btn-secondary");
                    break;
            }
        }
    }
    
    public enum ButtonStyle {
        PRIMARY, SECONDARY
    }
}
