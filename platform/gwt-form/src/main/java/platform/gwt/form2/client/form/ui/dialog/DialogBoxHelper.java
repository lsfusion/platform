package platform.gwt.form2.client.form.ui.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

public class DialogBoxHelper {
    public static interface CloseCallback {
        public void closed(OptionType chosenOption);
    }

    public static enum OptionType {
        YES, NO, CLOSE;

        public String getCaption() {
            //todo: localize
            switch (this) {
                case YES: return "Yes";
                case NO: return "No";
                case CLOSE: return "Close";
            }
            throw new IllegalStateException("Shouldn't happen");
        }

        /**
         * see {@link javax.swing.JOptionPane} (YES_OPTION, NO_OPTION)
         */
        public int asInteger() {
            switch (this) {
                case YES: return 0;
                case NO: return 1;
                case CLOSE: return 0;
            }
            throw new IllegalStateException("Shouldn't happen");
        }
    }

    public static void showMessageBox(boolean isError, String caption, String message, final CloseCallback closeCallback) {
        new MessageBox(caption, message, closeCallback, OptionType.CLOSE).showCenter();
    }

    public static void showConfirmBox(String caption, String message, final CloseCallback closeCallback) {
        new MessageBox(caption, message, closeCallback, OptionType.YES, OptionType.NO).showCenter();
    }

    private static final class MessageBox extends DialogBox {
        private HTML messageLabel;
        private FlowPanel buttonPane;
        private Button lastButton;

        private MessageBox(String caption, String message, final CloseCallback closeCallback, OptionType... options) {

            createMessagePanel(message);

            createButtonsPanel(options, closeCallback);

            final VerticalPanel mainPane = new VerticalPanel();
            mainPane.add(messageLabel);
            mainPane.add(buttonPane);
            mainPane.setCellHorizontalAlignment(buttonPane, HasAlignment.ALIGN_CENTER);

            setGlassEnabled(true);
            setText(caption);
            setWidget(mainPane);
        }

        private void createMessagePanel(String message) {
            messageLabel = new HTML(message);
            messageLabel.addStyleName("messageBox-message");
        }

        private void createButtonsPanel(OptionType[] options, CloseCallback closeCallback) {
            buttonPane = new FlowPanel();
            for (OptionType option : options) {
                lastButton = createOptionButton(option, closeCallback);
                buttonPane.add(lastButton);
            }
        }

        private Button createOptionButton(final OptionType option, final CloseCallback closeCallback) {
            Button optionButton = new Button(option.getCaption(), new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    hide();
                    if (closeCallback != null) {
                        closeCallback.closed(option);
                    }
                }
            });
            optionButton.addStyleName("messageBox-button");

            return optionButton;
        }

        public void showCenter() {
            super.center();
            lastButton.getElement().focus();
        }
    }
}
