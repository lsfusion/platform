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

        final VerticalPanel panel = new VerticalPanel();
        panel.add(new HTML(message));
        final Button buttonClose = new Button("Close",new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                box.hide();
            }
        });

        final HTML emptyLabel = new HTML("");
        emptyLabel.setSize("auto","25px");
        panel.add(emptyLabel);
        panel.add(emptyLabel);
        buttonClose.setWidth("90px");
        panel.add(buttonClose);
        panel.setCellHorizontalAlignment(buttonClose, HasAlignment.ALIGN_RIGHT);

        box.setText(caption);
        box.add(panel);
        box.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent) {
                if (closeCallback != null) {
                    closeCallback.closed(false);
                }
            }
        });

        box.center();
    }

    public static void showConfirmBox(String caption, String message, final CloseCallback closeCallback) {

    }
}
