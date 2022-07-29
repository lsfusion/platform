package lsfusion.gwt.client.base.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

public class FormButton extends Button {
    public FormButton() {
        super();
        setStyleName("btn");
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
                    addStyleName("btn-primary");
                    break;
                case SECONDARY:
                    addStyleName("btn-secondary");
                    break;
            }
        }
    }
    
    public enum ButtonStyle {
        PRIMARY, SECONDARY
    }
}
