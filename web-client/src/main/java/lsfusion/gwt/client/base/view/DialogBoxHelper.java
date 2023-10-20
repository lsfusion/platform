package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;

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

    public static void showMessageBox(String caption, String message, final CloseCallback closeCallback) {
        new MessageBox(caption, message, 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).show();
    }

    public static void showMessageBox(String caption, Widget contents, final CloseCallback closeCallback) {
        new MessageBox(caption, contents, 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).show();
    }

    public static void showConfirmBox(String caption, String message, boolean cancel, final CloseCallback closeCallback) {
        showConfirmBox(caption, message, cancel, 0, 0, closeCallback);
    }

    public static void showConfirmBox(String caption, String message, boolean cancel, int timeout, int initialValue, final CloseCallback closeCallback) {
        OptionType[] options = {OptionType.YES, OptionType.NO};
        if (cancel)
            options = new OptionType[]{OptionType.YES, OptionType.NO, OptionType.CLOSE};
        MessageBox messageBox = new MessageBox(caption, message, timeout, closeCallback, options[initialValue], options);
        messageBox.show();
    }
    
    public static MessageBox showConfirmBox(String caption, Widget contents, OptionType[] options, final CloseCallback closeCallback) {
        MessageBox messageBox = new MessageBox(caption, contents, 0, closeCallback, options[0], options);
        messageBox.show();
        return messageBox;
    }

    @SuppressWarnings("GWTStyleCheck")
    public static final class MessageBox extends DialogModalWindow {
        private final HandlerRegistration nativePreviewHandlerRegistration;
        private final CloseCallback closeCallback;
        private FormButton activeButton;

        private MessageBox(String caption, String message, int timeout, final CloseCallback closeCallback, OptionType activeOption, OptionType... options) {
            this(caption, new HTML(EscapeUtils.toHtml(message)), timeout, closeCallback, activeOption, options);
        }

        private MessageBox(String caption, Widget contents, int timeout, final CloseCallback closeCallback, final OptionType activeOption, OptionType... options) {
            super(caption, false, ModalWindowSize.FIT_CONTENT);

            this.closeCallback = closeCallback;

            setBodyWidget(contents);

            Style contentsContainerStyle = contents.getElement().getStyle();
            contentsContainerStyle.setProperty("overflow", "auto");
            contentsContainerStyle.setProperty("maxWidth", (Window.getClientWidth() * 0.75) + "px");
            contentsContainerStyle.setProperty("maxHeight", (Window.getClientHeight() * 0.75) + "px");

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

            nativePreviewHandlerRegistration = Event.addNativePreviewHandler(event -> {
                if (Event.ONKEYDOWN == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        GwtClientUtils.stopPropagation(event.getNativeEvent());
                        hide(OptionType.CLOSE);
                    }
                }
            });
        }

        private void createButtonsPanel(OptionType activeOption, OptionType[] options) {
            for (OptionType option : options) {
                FormButton optionButton = createOptionButton(option);
                if (option == activeOption) {
                    activeButton = optionButton;
                }
                addFooterWidget(optionButton);
            }
        }

        private FormButton createOptionButton(final OptionType option) {
            FormButton optionButton = new FormButton(option.getCaption(), event -> hide(option));
            optionButton.setStyle(option.isPrimary() ? FormButton.ButtonStyle.PRIMARY : FormButton.ButtonStyle.SECONDARY);

            return optionButton;
        }
        
        private void hide(final OptionType option) {
            hide();
            if (closeCallback != null) {
                closeCallback.closed(option);
            }
        }

        public void hide() {
            super.hide();
            if (nativePreviewHandlerRegistration != null) {
                nativePreviewHandlerRegistration.removeHandler();
            }
        }

        public void show() {
            super.show();
            FocusUtils.focus(activeButton.getElement(), FocusUtils.Reason.SHOW);
        }
    }
}
