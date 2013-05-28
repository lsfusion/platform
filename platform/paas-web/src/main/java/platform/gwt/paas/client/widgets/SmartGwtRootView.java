package platform.gwt.paas.client.widgets;

import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.RootPresenter;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.layout.HLayout;

public class SmartGwtRootView extends RootPresenter.RootView {

    public HLayout rootWidget;
    public Canvas currentContent;

    public SmartGwtRootView() {
        rootWidget = new HLayout();
        rootWidget.setWidth100();
        rootWidget.setHeight100();

        rootWidget.draw();
    }

    @Override
    public void setInSlot(Object slot, Widget content) {
        if (currentContent != null) {
            rootWidget.removeChild(currentContent);
        }

        if (content != null) {
            if (content instanceof Canvas) {
                currentContent = (Canvas) content;
            } else {
                currentContent = new WidgetCanvas(content);
            }
            rootWidget.addChild(content);
        } else {
            currentContent = null;
        }
    }
}
