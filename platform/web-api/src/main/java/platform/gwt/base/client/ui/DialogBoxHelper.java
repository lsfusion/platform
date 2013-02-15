package platform.gwt.base.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.base.client.EscapeUtils;

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
        new MessageBox(caption, message, closeCallback, OptionType.CLOSE, OptionType.CLOSE).showCenter();
    }

    public static void showMessageBox(boolean isError, String caption, Widget contents, final CloseCallback closeCallback) {
        new MessageBox(caption, contents, closeCallback, OptionType.CLOSE, OptionType.CLOSE).showCenter();
    }

    public static void showConfirmBox(String caption, String message, final CloseCallback closeCallback) {
        new MessageBox(caption, message, closeCallback, OptionType.YES, OptionType.YES, OptionType.NO).showCenter();
    }

    private static final class MessageBox extends DialogBox {
        private Widget contents;
        private FlowPanel buttonPane;
        private Button activeButton;

        private MessageBox(String caption, String message, final CloseCallback closeCallback, OptionType activeOption, OptionType... options) {
            this(caption, new HTML(EscapeUtils.toHtml(message)), closeCallback, activeOption, options);
        }

        private MessageBox(String caption, Widget contents, final CloseCallback closeCallback, OptionType activeOption, OptionType... options) {
            this.contents = contents;
            this.contents.addStyleName("messageBox-message");

            createButtonsPanel(activeOption, options, closeCallback);

            final VerticalPanel mainPane = new VerticalPanel();
            mainPane.add(this.contents);
            mainPane.add(buttonPane);
            mainPane.setCellHorizontalAlignment(buttonPane, HasAlignment.ALIGN_CENTER);

            setGlassEnabled(true);
            setModal(true);

            setText(caption);
            setWidget(mainPane);
        }

        private void createButtonsPanel(OptionType activeOption, OptionType[] options, CloseCallback closeCallback) {
            buttonPane = new FlowPanel();
            for (OptionType option : options) {
                Button optionButton = createOptionButton(option, closeCallback);
                if (option == activeOption) {
                    activeButton = optionButton;
                }
                buttonPane.add(optionButton);
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
            activeButton.getElement().focus();
        }
    }
}
