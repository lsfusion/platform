package lsfusion.gwt.client.base.exception;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.DialogModalWindow;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;

public class ErrorDialog extends DialogModalWindow {
    private HandlerRegistration nativePreviewHandlerRegistration;
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final FormButton closeButton;

    private FlexTabbedPanel stacks = null;

    public ErrorDialog(String caption, String messageHTML, String javaStack, String lsfStack) {
        super(caption, false, ModalWindowSize.EXTRA_LARGE);

        ResizableComplexPanel body = new ResizableComplexPanel();

        Widget message = new HTML(messageHTML);
        body.add(message);

        Style messageStyle = message.getElement().getStyle();
        messageStyle.setProperty("overflow", "auto");
        messageStyle.setProperty("maxWidth", (Window.getClientWidth() * 0.9) + "px");
        messageStyle.setProperty("maxHeight", (Window.getClientHeight() * 0.3) + "px");

        if (javaStack != null || lsfStack != null) {
            stacks = new FlexTabbedPanel();
            if (javaStack != null) {
                TextArea javaTA = new TextArea();
                javaTA.setStyleName("dialog-error-stack form-control");
                javaTA.setText(javaStack);
                stacks.addTab(javaTA, "Java");
            }
            if (lsfStack != null) {
                TextArea lsfTA = new TextArea();
                lsfTA.setStyleName("dialog-error-stack form-control");
                lsfTA.setText(lsfStack);
                stacks.addTab(lsfTA, "LSF");
            }
            stacks.selectTab(0);
            stacks.setVisible(false);

            body.add(stacks);
        }

        setBodyWidget(body);

        if(stacks != null) {
            FormButton copyToClipboardButton = new FormButton(messages.copyToClipboard(), FormButton.ButtonStyle.SECONDARY, event -> {
                CopyPasteUtils.copyToClipboard(messageHTML +
                        (isRedundantString(javaStack) ? "" : ("\n" + javaStack)) +
                        (isRedundantString(lsfStack) ? "" : ("\n" + lsfStack)));
            });
            addFooterWidget(copyToClipboardButton);
        }

        closeButton = new FormButton(messages.close(), FormButton.ButtonStyle.PRIMARY, event -> hide());
        addFooterWidget(closeButton);

        if (stacks != null) {
            FormButton moreButton = new FormButton(messages.more(), FormButton.ButtonStyle.SECONDARY, event -> stacks.setVisible(!stacks.isVisible()));
            addFooterWidget(moreButton);
        }

        nativePreviewHandlerRegistration = Event.addNativePreviewHandler(event -> {
            if (Event.ONKEYDOWN == event.getTypeInt()) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                    GwtClientUtils.stopPropagation(event.getNativeEvent());
                    hide();
                }
            }
        });
    }

    @Override
    public void show(Widget popupOwnerWidget) {
        super.show(popupOwnerWidget);

        closeButton.setFocus(true);
    }

    public static void show(String caption, String message, String javaStack, String lsfStack, Widget popupOwnerWidget) {
        new ErrorDialog(caption, message, javaStack, lsfStack).show(popupOwnerWidget);
    }

    @Override
    public void hide() {
        super.hide();
        if (nativePreviewHandlerRegistration != null) {
            nativePreviewHandlerRegistration.removeHandler();
        }
    }
}
