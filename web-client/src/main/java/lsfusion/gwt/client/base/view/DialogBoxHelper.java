package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.event.GKeyStroke;

public class DialogBoxHelper {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    public interface CloseCallback {
        void closed(OptionType chosenOption);
    }

    public enum OptionType {
        YES, NO, OK, CANCEL, CLOSE;

        public String getCaption() {
            switch (this) {
                case YES: return messages.yes();
                case NO: return messages.no();
                case OK: return messages.ok();
                case CANCEL: return messages.cancel();
                case CLOSE: return messages.close();
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
                case OK: return 0;
                case CANCEL: return 1;
                case CLOSE: return 2;
            }
            throw new IllegalStateException("Shouldn't happen");
        }

        public boolean isPrimary() {
            return this.equals(YES) || this.equals(OK);
        }
    }

    public static void showMessageBox(boolean isError, String caption, String message, final CloseCallback closeCallback) {
        showMessageBox(isError, caption, message, true, closeCallback);
    }

    public static void showMessageBox(boolean isError, String caption, String message, boolean escapeMessage, final CloseCallback closeCallback) {
        new MessageBox(caption, escapedIf(message, escapeMessage), 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).show();
    }

    private static String escapedIf(String message, boolean escapeMessage) {
        return escapeMessage ? EscapeUtils.toHtml(message) : message;
    }

    public static void showMessageBox(boolean isError, String caption, Widget contents, final CloseCallback closeCallback) {
        new MessageBox(caption, contents, 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).show();
    }

    public static void showConfirmBox(String caption, String message, boolean cancel, final CloseCallback closeCallback) {
        showConfirmBox(caption, message, cancel, 0, 0, true, closeCallback);
    }

    public static void showConfirmBox(String caption, String message, boolean cancel, int timeout, int initialValue, final CloseCallback closeCallback) {
        showConfirmBox(caption, message, cancel, timeout, initialValue, true, closeCallback);
    }

    public static void showConfirmBox(String caption, String message, boolean cancel, int timeout, int initialValue, boolean escapeMessage, final CloseCallback closeCallback) {
        OptionType[] options = {OptionType.YES, OptionType.NO};
        if (cancel)
            options = new OptionType[]{OptionType.YES, OptionType.NO, OptionType.CLOSE};
        MessageBox messageBox = new MessageBox(caption, escapedIf(message, escapeMessage), timeout, closeCallback, options[initialValue], options);
        messageBox.show();
    }
    
    public static MessageBox showConfirmBox(String caption, Widget contents, OptionType[] options, final CloseCallback closeCallback) {
        MessageBox messageBox = new MessageBox(caption, contents, 0, closeCallback, options[0], options);
        messageBox.show();
        return messageBox;
    }
    
    @SuppressWarnings("GWTStyleCheck")
    public static final class MessageBox extends DialogModalWindow {
        private final CloseCallback closeCallback;
        private Button activeButton;

        private MessageBox(String caption, String message, int timeout, final CloseCallback closeCallback, OptionType activeOption, OptionType... options) {
            this(caption, new HTML(message), timeout, closeCallback, activeOption, options);
        }

        private MessageBox(String caption, Widget contents, int timeout, final CloseCallback closeCallback, final OptionType activeOption, OptionType... options) {
            super(false, true);

            this.closeCallback = closeCallback;

            setCaption(caption);
            setBodyWidget(contents);

            createButtonsPanel(activeOption, options);

            if (timeout != 0) {
                final Timer timer = new Timer() {
                    @Override
                    public void run() {
                        hide(activeOption);
                    }
                };
                timer.schedule(timeout);
            }
        }

        private void createButtonsPanel(OptionType activeOption, OptionType[] options) {
            for (OptionType option : options) {
                Button optionButton = createOptionButton(option);
                if (option == activeOption) {
                    activeButton = optionButton;
                }
                addFooterWidget(optionButton);
            }
        }

        private Button createOptionButton(final OptionType option) {
            Button optionButton = new Button(option.getCaption(), (ClickHandler) event -> hide(option));
            optionButton.setStyleName("btn");
            optionButton.addStyleName("btn-" + (option.isPrimary() ? "primary" : "secondary"));

            return optionButton;
        }
        
        private void hide(final OptionType option) {
            hide();
            if (closeCallback != null) {
                closeCallback.closed(option);
            }
        }

        public void show() {
            super.show();
            activeButton.getElement().focus();
        }
    }
}
