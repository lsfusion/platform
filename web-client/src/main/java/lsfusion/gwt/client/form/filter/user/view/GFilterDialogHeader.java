package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.view.StyleDefaults;

public class GFilterDialogHeader extends FlowPanel implements DialogBox.Caption {
    private Label captionWidget;
    private HandlerManager handlerManager;

    public GFilterDialogHeader(String caption) {
        super();

        setStyleName("Caption");
        addStyleName("filterDialogHeader");
        setHeight(StyleDefaults.COMPONENT_HEIGHT_STRING);

        captionWidget = new Label();
        captionWidget.addStyleName("flowPanelChildLeftAlign");
        captionWidget.addStyleName("filterDialogCaption");
        setText(caption);

        add(captionWidget);

        handlerManager = new HandlerManager(this);
    }

    @Override
    public String getHTML() {
        return "";
    }

    @Override
    public void setHTML(String html) {
    }

    @Override
    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
        return handlerManager.addHandler(MouseDownEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
        return handlerManager.addHandler(MouseMoveEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return handlerManager.addHandler(MouseOutEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
        return handlerManager.addHandler(MouseOverEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
        return handlerManager.addHandler(MouseUpEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
        return handlerManager.addHandler(MouseWheelEvent.getType(), handler);
    }

    @Override
    public void setHTML(SafeHtml html) {
    }

    @Override
    public String getText() {
        return captionWidget.getText();
    }

    @Override
    public void setText(String text) {
        captionWidget.setText(EscapeUtils.unicodeEscape(text));
    }
}