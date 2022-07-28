package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ModalWindow {

    private String tooltip;

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public ResizableModalWindow() {
        super(true, ModalWindowSize.FIT_CONTENT);

        TooltipManager.registerWidget(header, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return tooltip;
            }

            @Override
            public boolean stillShowTooltip() {
                return header.isAttached() && header.isVisible();
            }
        });
    }

    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex) {
        this.formRequestData = formRequestData;
        show(insertIndex);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = RootPanel.get();
        for (int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            if (widget instanceof ResizableModalWindow) {
                FormRequestData widgetData = ((ResizableModalWindow) widget).formRequestData;
                if (widgetData.isBefore(formRequestData)) {
                    return new Pair(widgetData.modalForm, i);
                }
            }
        }
        return null;
    }

    public void hide() {
        super.hide();
        formRequestData = null;
    }

    public void setContentSize(Dimension size) {
        Element element = getContentWidget().getElement();
        FlexPanel.setWidth(element, size.width);
        FlexPanel.setHeight(element, size.height);
    }

    public void focus() {
        Element element = RootPanel.get().getElement();
        int tabIndex = element.getTabIndex();
        if (tabIndex == -1) {
            element.setTabIndex(0);
        }
        FocusUtils.focus(element, FocusUtils.Reason.SHOW);
    }
}
