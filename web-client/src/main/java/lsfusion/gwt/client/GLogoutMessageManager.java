package lsfusion.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ui.WindowBox;

public class GLogoutMessageManager {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private static GBlockDialog blockDialog;

    public static void start(String message) {
        blockDialog = new GBlockDialog(message);
        new Timer() {
            @Override
            public void run() {
                blockDialog.showDialog();
            }
        }.schedule(1000);
    }

    public static class GBlockDialog extends WindowBox {

        private Button btnLogout;
        private HTML lbMessage;

        public GBlockDialog(String message) {
            super(false, true, false, false, false);
            setGlassEnabled(true);
            lbMessage = new HTML(message);

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            buttonPanel.setSpacing(5);

            btnLogout = new Button(messages.checkApiVersionLogout());
            btnLogout.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    exitAction();
                }
            });
            buttonPanel.add(btnLogout);

            setModal(true);

            setText(messages.checkApiVersionTitle());

            HorizontalPanel centralPanel = new HorizontalPanel();
            centralPanel.add(lbMessage);

            VerticalPanel mainPanel = new VerticalPanel();
            mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            mainPanel.add(centralPanel);
            mainPanel.add(buttonPanel);

            setWidget(mainPanel);
        }

        public void showDialog() {
            center();
        }

        private void exitAction() {
            GwtClientUtils.logout();
        }
    }
}