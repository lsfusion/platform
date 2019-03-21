package lsfusion.gwt.client.form.object.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class GShowTypeView extends ResizableHorizontalPanel {
    private static final ClientMessages messages = ClientMessages.Instance.get();
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
    public Dimension getMaxPreferredSize() {
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
