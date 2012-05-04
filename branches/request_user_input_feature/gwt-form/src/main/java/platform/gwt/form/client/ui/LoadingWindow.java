package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.ViewLoader;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;

public class LoadingWindow extends Window {
    private LoadingWindow() {
        setWidth(360);
        setHeight(115);
        setTitle("Загрузка...");
        setShowMinimizeButton(false);
        setShowCloseButton(false);
        setIsModal(true);
        setShowModalMask(true);
        centerInPage();
        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                destroy();
            }
        });

        ViewLoader loader = new ViewLoader();
        loader.setIcon("loading.gif");
        loader.setLoadingMessage("Загрузка...");
        addItem(loader);
    }

    public static LoadingWindow showLoadingBlocker() {
        LoadingWindow wl = new LoadingWindow();
        wl.show();
        return wl;
    }
}
