package lsfusion.gwt.client.base.exception;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

public class ErrorDialog extends WindowBox {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private Button closeButton;
    private FlexTabbedPanel stacksPanel = null;

    public ErrorDialog(String caption, String message, String javaStack, String lsfStack) {
        super(false, false, true);
        setModal(true);
        setGlassEnabled(true);

        setText(caption);

        VerticalPanel mainPanel = new VerticalPanel();

        FlexPanel topPanel = new FlexPanel(true);
        topPanel.setHeight("100%");
        mainPanel.add(topPanel);

        Widget messageWidget = new HTML(message);
        messageWidget.addStyleName("errorBox-message");
        topPanel.add(messageWidget);

        if (javaStack != null || lsfStack != null) {
            stacksPanel = new FlexTabbedPanel();
            if (javaStack != null) {
                TextArea javaTA = new TextArea();
                javaTA.addStyleName("errorBox-stackBox");
                javaTA.setText(javaStack);
                stacksPanel.addTab(javaTA, "Java");
            }
            if (lsfStack != null) {
                TextArea lsfTA = new TextArea();
                lsfTA.addStyleName("errorBox-stackBox");
                lsfTA.setText(lsfStack);
                stacksPanel.addTab(lsfTA, "LSF");
            }
            stacksPanel.selectTab(0);
            stacksPanel.setVisible(false);

            topPanel.addFill(stacksPanel);
        }

        FlexPanel buttonsPanel = new FlexPanel(false, GFlexAlignment.CENTER);
        mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
        mainPanel.add(buttonsPanel);

        closeButton = new Button(messages.close(), (ClickHandler) event -> hide());
        closeButton.addStyleName("errorBox-button");
        buttonsPanel.add(closeButton);

        if (stacksPanel != null) {
            Button moreButton = new Button(messages.more(), (ClickHandler) event -> stacksPanel.setVisible(!stacksPanel.isVisible()));
            moreButton.addStyleName("errorBox-button");
            buttonsPanel.add(moreButton);
        }
        
        mainPanel.getElement().getStyle().setProperty("maxWidth", (int) (Window.getClientWidth() * 0.9) + "px");
        mainPanel.getElement().getStyle().setProperty("maxHeight", (int) (Window.getClientHeight() * 0.9) + "px");

        mainPanel.addDomHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                GwtClientUtils.stopPropagation(event);
                hide();
            }
        }, KeyDownEvent.getType());

        setWidget(mainPanel);
        center();
    }

    @Override
    public void show() {
        super.show();

        closeButton.setFocus(true);
    }

    public static void show(String caption, String message, String javaStack, String lsfStack) {
        new ErrorDialog(caption, message, javaStack, lsfStack).show();
    }
}
