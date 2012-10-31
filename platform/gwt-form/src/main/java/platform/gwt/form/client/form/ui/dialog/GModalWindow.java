package platform.gwt.form.client.form.ui.dialog;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.PopupPanel;

public class GModalWindow extends DialogBox {
    private WindowHiddenHandler hiddenHandler;

    public GModalWindow(String caption) {
        this(caption, null);
    }

    public GModalWindow(String caption, WindowHiddenHandler ihiddenHandler) {
        this.hiddenHandler = ihiddenHandler;

        setText(caption);
        setGlassEnabled(true);

        addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                hiddenHandler.onHidden();
            }
        });
    }

    public void setWindowHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }
}
