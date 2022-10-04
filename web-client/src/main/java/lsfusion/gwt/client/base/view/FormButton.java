package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;

public class FormButton extends ButtonBase {
    public FormButton(Element element) {
        super(element == null ? Document.get().createPushButtonElement() : element);
        if (element == null)
            setStyleName("btn");
    }

    public FormButton() {
        this(null);
    }

    public FormButton(Element element, String html) {
        this(element);
        setHTML(html);
    }
    
    public FormButton(String html, ButtonStyle style) {
        this(null, html);
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
