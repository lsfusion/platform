package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

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

    public static void showMessageBox(String caption, String message, PopupOwner popupOwner, final CloseCallback closeCallback) {
        showMessageBox(caption, new HTML(message), popupOwner, closeCallback);
    }

    public static void showMessageBox(String caption, Widget contents, PopupOwner popupOwner, final CloseCallback closeCallback) {
        showMessageBox(caption, contents, null, popupOwner, closeCallback);
    }

    public static void showMessageBox(String caption, Widget contents, String backgroundClass, PopupOwner popupOwner, final CloseCallback closeCallback) {
        new MessageBox(caption, contents, backgroundClass, 0, closeCallback, OptionType.CLOSE, OptionType.CLOSE).show(popupOwner);
    }

    public static void showConfirmBox(String caption, String message, PopupOwner popupOwner, final CloseCallback closeCallback) {
        showConfirmBox(caption, new HTML(message), popupOwner, closeCallback);
    }

    public static void showConfirmBox(String caption, Widget widget, PopupOwner popupOwner, final CloseCallback closeCallback) {
        showConfirmBox(caption, widget, false, 0, 0, popupOwner, closeCallback);
    }

    public static void showConfirmBox(String caption, Widget contents, boolean cancel, int timeout, int initialValue, PopupOwner popupOwner, final CloseCallback closeCallback) {
        showConfirmBox(caption, contents, timeout, cancel ? new OptionType[]{OptionType.YES, OptionType.NO, OptionType.CLOSE} : new OptionType[] {OptionType.YES, OptionType.NO}, initialValue, popupOwner, closeCallback);
    }
    
    public static MessageBox showConfirmBox(String caption, Widget contents, OptionType[] options, PopupOwner popupOwner, final CloseCallback closeCallback) {
        return showConfirmBox(caption, contents, 0, options, 0, popupOwner, closeCallback);
    }
    public static MessageBox showConfirmBox(String caption, Widget contents, int timeout, OptionType[] options, int initialValue, PopupOwner popupOwner, final CloseCallback closeCallback) {
        MessageBox messageBox = new MessageBox(caption, contents, timeout, closeCallback, options[initialValue], options);
        messageBox.show(popupOwner);
        return messageBox;
    }

    @SuppressWarnings("GWTStyleCheck")
    public static final class MessageBox extends DialogModalWindow {
        private final HandlerRegistration nativePreviewHandlerRegistration;
        private final CloseCallback closeCallback;
        private FormButton activeButton;

        private MessageBox(String caption, Widget contents, int timeout, final CloseCallback closeCallback, final OptionType activeOption, OptionType... options) {
            this(caption, contents, null, timeout, closeCallback, activeOption, options);
        }

        private MessageBox(String caption, Widget contents, String backgroundClass, int timeout, final CloseCallback closeCallback, final OptionType activeOption, OptionType... options) {
            super(caption, false, ModalWindowSize.FIT_CONTENT, backgroundClass);
            
            GwtClientUtils.addClassName(dialog, "modal-dialog-scrollable");

            this.closeCallback = closeCallback;

            setBodyWidget(contents);

//            Style contentsContainerStyle = contents.getElement().getStyle();
//            contentsContainerStyle.setProperty("maxWidth", (Window.getClientWidth() * 0.75) + "px");
//            contentsContainerStyle.setProperty("maxHeight", (Window.getClientHeight() * 0.75) + "px");

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

            if (!MainFrame.useBootstrap)
                GwtClientUtils.addShowCollapsedContainerEvent(getElement(),
                        "span.text-primary.highlight-text", "span#collapseTextId", "collapsible-text");

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
                    activeButton.setTabIndex(0); // need this to set focus after
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

        public void show(PopupOwner popupOwner) {
            super.show(popupOwner);
            Element element = activeButton.getElement();
            FocusUtils.focus(element, FocusUtils.Reason.SHOW);
        }
    }
}
