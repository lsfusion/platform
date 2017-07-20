package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.client.form.ui.toolbar.GToolbarButton;
import lsfusion.gwt.form.shared.view.GClassViewType;
import lsfusion.gwt.form.shared.view.GGroupObject;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.base.client.GwtClientUtils.getOffsetSize;

public class GShowTypeView extends ResizableHorizontalPanel {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private Button gridButton;
    private Button panelButton;
    private Button hideButton;
    
    private GClassViewType classView = GClassViewType.HIDE;
    private List<GClassViewType> banClassViews;
    private final GGroupObject groupObject;
    private final GFormController form;

    public GShowTypeView(final GFormController iform, final GGroupObject igroupObject) {
        form = iform;
        groupObject = igroupObject;

        add(gridButton = createShowTypeButton("view_grid.png", GClassViewType.GRID, messages.grid()));
        add(panelButton = createShowTypeButton("view_panel.png", GClassViewType.PANEL, messages.panel()));
        add(hideButton = createShowTypeButton("view_hide.png", GClassViewType.HIDE, messages.hide()));
    }

    private Button createShowTypeButton(String imagePath, final GClassViewType newClassView, String tooltipText) {
        return new GToolbarButton(imagePath, tooltipText) {
            @Override
            public void addListener() {
                addClickHandler(new ChangeViewBtnClickHandler(newClassView));
            }
        };
    }

    public boolean setClassView(GClassViewType newClassView) {
        if (newClassView != classView) {
            classView = newClassView;

            gridButton.setEnabled(classView != GClassViewType.GRID);
            panelButton.setEnabled(classView != GClassViewType.PANEL);
            hideButton.setEnabled(classView != GClassViewType.HIDE);

            setVisible(banClassViews.size() < 2);

            return true;
        }
        return false;
    }

    public void setBanClassViews(List<String> banClassViewNames) {
        List<GClassViewType> banClassViews = new ArrayList<>();
        for (String banClassViewName : banClassViewNames) {
            banClassViews.add(GClassViewType.valueOf(banClassViewName));
        }

        this.banClassViews = banClassViews;
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(groupObject.showType, this);
    }

    public void update(GClassViewType classView) {
        setClassView(classView);
    }

    @Override
    public Dimension getPreferredSize() {
        return getOffsetSize(this);
    }

    private class ChangeViewBtnClickHandler implements ClickHandler {
        private final GClassViewType newClassView;

        public ChangeViewBtnClickHandler(GClassViewType newClassView) {
            this.newClassView = newClassView;
        }

        @Override
        public void onClick(ClickEvent event) {
            if (classView != newClassView) {
                form.changeClassView(groupObject, newClassView);
            }
        }
    }
}
