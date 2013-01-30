package platform.gwt.form.client.form.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import platform.gwt.base.client.ui.ResizableHorizontalPanel;
import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.panel.ImageButton;

import java.util.ArrayList;
import java.util.List;

public class GShowTypeView extends ResizableHorizontalPanel {
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

        addStyleName("showType");

        add(gridButton = createShowTypeButton("view_grid.png", GClassViewType.GRID));
        add(panelButton = createShowTypeButton("view_panel.png", GClassViewType.PANEL));
        add(hideButton = createShowTypeButton("view_hide.png", GClassViewType.HIDE));
    }

    private Button createShowTypeButton(String imagePath, GClassViewType newClassView) {
        Button showTypeButton = new ImageButton(null, imagePath);
        showTypeButton.addStyleName("toolbarButton");
        showTypeButton.addClickHandler(new ChangeViewBtnClickHandler(newClassView));
        return showTypeButton;
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
        List<GClassViewType> banClassViews = new ArrayList<GClassViewType>();
        for (String banClassViewName : banClassViewNames) {
            banClassViews.add(GClassViewType.valueOf(banClassViewName));
        }

        this.banClassViews = banClassViews;
    }

    public boolean needToBeVisible() {
        return banClassViews.size() < 2;
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(groupObject.showType, this);
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
