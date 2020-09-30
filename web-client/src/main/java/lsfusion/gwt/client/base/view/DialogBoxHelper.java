package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.ScriptInjector;
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
        YES, NO, OK, CANCEL, CLOSE, LOGOUT;

        public String getCaption() {
            switch (this) {
                case YES: return messages.yes();
                case NO: return messages.no();
                case OK: return messages.ok();
                case CANCEL: return messages.cancel();
                case CLOSE: return messages.close();
                case LOGOUT: return messages.logout();
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
                case CLOSE: return 0;
                case LOGOUT: return 1;
            }
            throw new IllegalStateException("Shouldn't happen");
        }
    }

    public static void showMessageBox(boolean isError, String caption, String message, final CloseCallback closeCallback) {
        showMessageBox(isError, caption, message, true, closeCallback);
    }

    public static void showMessageBox(boolean isError, String caption, String message, boolean escapeMessage, final CloseCallback closeCallback) {
        new MessageBox(caption, escapedIf(message, escapeMessage), 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).showCenter();
    }

    private static String escapedIf(String message, boolean escapeMessage) {
        return escapeMessage ? EscapeUtils.toHtml(message) : message;
    }

    public static void showMessageBox(boolean isError, String caption, Widget contents, final CloseCallback closeCallback) {
        new MessageBox(caption, contents, 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).showCenter();
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
        messageBox.showCenter();
    }
    
    public static MessageBox showConfirmBox(String caption, Widget contents, OptionType[] options, final CloseCallback closeCallback) {
        MessageBox messageBox = new MessageBox(caption, contents, 0, closeCallback, options[0], options);
        messageBox.showCenter();   
        return messageBox;
    }
    
    public static void showLogoutMessageBox(String caption, String message, CloseCallback callback) {
        new MessageBox(caption, message, 0, callback, OptionType.CLOSE, OptionType.CLOSE, OptionType.LOGOUT).showCenter();
    }

    @SuppressWarnings("GWTStyleCheck")
    public static final class MessageBox extends DialogModalBox {
        private Widget contents;
        private CloseCallback closeCallback;
        private HorizontalPanel buttonPane;
        private Button activeButton;

        @Override
        protected void onLoad() {
            ScriptInjector.fromUrl("static/js/clipboard.js").inject();
        }

        private MessageBox(String caption, String message, int timeout, final CloseCallback closeCallback, OptionType activeOption, OptionType... options) {
            this(caption, new HTML(message), timeout, closeCallback, activeOption, options);
        }

        private MessageBox(String caption, Widget contents, int timeout, final CloseCallback closeCallback, final OptionType activeOption, OptionType... options) {
            this.contents = contents;
            this.closeCallback = closeCallback;

            ResizableSimplePanel contentsContainer = new ResizableSimplePanel(this.contents);
            contentsContainer.addStyleName("messageBox-messageContainer");
            Style contentsContainerStyle = contentsContainer.getElement().getStyle();
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

            final VerticalPanel mainPane = new VerticalPanel();
            mainPane.add(contentsContainer);
            mainPane.add(buttonPane);
            mainPane.setCellHorizontalAlignment(buttonPane, HasAlignment.ALIGN_CENTER);

            setGlassEnabled(true);

            setText(caption);
            setWidget(mainPane);
        }

        // модальный диалог не воспринимает некоторые события. нам не хватает ctrl+C 
        // поэтому на preview пытаемся руками затолкать в буфер обмена выделенный текст
        @Override
        protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
            // в IE работает и так
            if (!GwtClientUtils.isIEUserAgent() && GKeyStroke.isCopyToClipboardEvent(event.getNativeEvent())) {
                CopyPasteUtils.setClipboardData();
            }

            if (Event.ONKEYDOWN == event.getTypeInt()) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                    GwtClientUtils.stopPropagation(event.getNativeEvent());
                    hide(OptionType.CANCEL); // что-нибудь, преобразуемое в 1
                }
            }
            
            super.onPreviewNativeEvent(event);
        }

        private void createButtonsPanel(OptionType activeOption, OptionType[] options) {
            buttonPane = new HorizontalPanel();
            buttonPane.setSpacing(3);
            for (OptionType option : options) {
                Button optionButton = createOptionButton(option);
                if (option == activeOption) {
                    activeButton = optionButton;
                }
                buttonPane.add(optionButton);
            }
        }

        private Button createOptionButton(final OptionType option) {
            Button optionButton = new Button(option.getCaption(), new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    hide(option);
                }
            });
            optionButton.addStyleName("messageBox-button");

            return optionButton;
        }
        
        private void hide(final OptionType option) {
            hide();
            if (closeCallback != null) {
                closeCallback.closed(option);
            }
        }

        public void showCenter() {
            center();
            activeButton.getElement().focus();
        }
    }
}
