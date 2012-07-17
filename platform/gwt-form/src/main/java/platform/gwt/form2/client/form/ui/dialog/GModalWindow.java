package platform.gwt.form2.client.form.ui.dialog;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.VisibilityChangedEvent;
import com.smartgwt.client.widgets.events.VisibilityChangedHandler;
import platform.gwt.form2.client.form.ui.WindowHiddenHandler;

public class GModalWindow extends Window {
    private WindowHiddenHandler hiddenHandler;

    public GModalWindow(String caption) {
        this(caption, null);
    }

    public GModalWindow(String caption, WindowHiddenHandler ihiddenHandler) {
        this.hiddenHandler = ihiddenHandler;

        setTitle(caption);
        setShowMinimizeButton(false);
        setShowCloseButton(false);
        setShowModalMask(true);
        setIsModal(true);
        setCanDragResize(true);
        setCanDragReposition(true);
        setWidth(800);
        setHeight(600);
        setAutoCenter(true);
        setOverflow(Overflow.VISIBLE);

        addVisibilityChangedHandler(new VisibilityChangedHandler() {
            @Override
            public void onVisibilityChanged(VisibilityChangedEvent event) {
                if (!event.getIsVisible()) {
                    if (hiddenHandler != null) {
                        hiddenHandler.onHidden();
                    }
                } else {
                    setAutoSize(true);
                }
            }
        });
    }

    public void setWindowHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }
}
