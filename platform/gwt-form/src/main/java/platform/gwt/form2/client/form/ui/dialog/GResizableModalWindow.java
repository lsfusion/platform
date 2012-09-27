package platform.gwt.form2.client.form.ui.dialog;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;

public class GResizableModalWindow extends ResizableWindow {
    private ModalMask modalMask;
    private WindowHiddenHandler hiddenHandler;

    public GResizableModalWindow(String caption) {
        this(caption, null);
    }

    public GResizableModalWindow(String caption, WindowHiddenHandler ihiddenHandler) {
        super(caption);

        this.hiddenHandler = ihiddenHandler;

        addCloseHandler(new CloseHandler<ResizableWindow>() {
            @Override
            public void onClose(CloseEvent<ResizableWindow> event) {
                if (modalMask != null) {
                    modalMask.hide();
                    modalMask = null;
                }
                hiddenHandler.onHidden();
            }
        });
    }

    public void setWindowHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }

    @Override
    public void center() {
        modalMask = new ModalMask();
        modalMask.show();

        super.center();
    }

    static class ModalMask {
        private final PopupPanel popup;

        private ModalMask() {
            popup = new PopupPanel();
            popup.setGlassEnabled(true);
            popup.getElement().getStyle().setOpacity(0);
        }

        public void show() {
            popup.center();
        }

        public void hide() {
            popup.hide();
        }
    }
}
