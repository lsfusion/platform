package platform.gwt.form2.client.form.ui.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.*;

public class MessageBox {
    public static interface CloseCallback {
        public void closed(boolean okPressed);
    }

    public static void showMessageBox(boolean isError, String caption, String message, final CloseCallback closeCallback) {
        final DialogBox box = new DialogBox();

        final Button buttonClose = new Button("Close", new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                box.hide();
            }
        });
        buttonClose.addStyleName("messageBox-button");

        HTML messageLabel = new HTML(message);
        messageLabel.addStyleName("messageBox-message");

        final VerticalPanel panel = new VerticalPanel();
        panel.add(messageLabel);
        panel.add(buttonClose);
        panel.setCellHorizontalAlignment(buttonClose, HasAlignment.ALIGN_RIGHT);

        box.setText(caption);
        box.setWidget(panel);
        box.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent) {
                if (closeCallback != null) {
                    closeCallback.closed(false);
                }
            }
        });

        box.center();

        buttonClose.getElement().focus();
    }

    public static void showConfirmBox(String caption, String message, final CloseCallback closeCallback) {

    }
}
