package platform.gwt.form2.client.form.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.form2.shared.view.GGroupObject;

import java.util.ArrayList;
import java.util.List;

public class GShowTypeView extends HorizontalPanel {
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

        add(gridButton = createShowTypeButton("G", GClassViewType.GRID));
        add(panelButton = createShowTypeButton("P", GClassViewType.PANEL));
        add(hideButton = createShowTypeButton("H", GClassViewType.HIDE));
    }

    private Button createShowTypeButton(String label, GClassViewType newClassView) {
        Button showTypeButton = new Button(label);
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

            if (classView == GClassViewType.HIDE) {
                needToBeHidden();
            } else {
                needToBeShown();
            }

            return true;
        }
        return false;
    }

    protected void needToBeShown() {
    }

    protected void needToBeHidden() {
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
