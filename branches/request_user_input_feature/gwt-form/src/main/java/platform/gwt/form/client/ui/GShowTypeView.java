package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HStack;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.view.GGroupObject;

import java.util.List;

public abstract class GShowTypeView extends HStack {
    private IButton gridButton;
    private IButton panelButton;
    private IButton hideButton;
    
    private GClassViewType classView = GClassViewType.HIDE;
    private List<GClassViewType> banClassView;

    public GShowTypeView(final GGroupObject groupObject, final GFormController form) {
        setHeight(1);
        setBackgroundColor("#F5F5F5");

        ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String viewName = ((IButton) event.getSource()).getTitle();
                viewName = viewName.replace("G", "GRID").replace("P", "PANEL").replace("H", "HIDE");  //переделать на не титулы
                GClassViewType newClassView = GClassViewType.valueOf(viewName);
                if (!classView.equals(newClassView)) {
                    form.changeClassView(groupObject, newClassView);
                }
            }
        };
        gridButton = new IButton("G", clickHandler);    //забацать иконки
        gridButton.setWidth("20px");
        panelButton = new IButton("P", clickHandler);
        panelButton.setWidth("20px");
        hideButton = new IButton("H", clickHandler);
        hideButton.setWidth("20px");

        addMember(gridButton);
        addMember(panelButton);
        addMember(hideButton);
    }
    
    public boolean changeClassView(GClassViewType newClassView) {

        switch (newClassView) {
            case GRID:
                gridButton.disable();
                panelButton.enable();
                hideButton.enable();
                show();
                break;
            case PANEL:
                gridButton.enable();
                panelButton.disable();
                hideButton.enable();
                show();
                break;
            case HIDE:
                gridButton.enable();
                panelButton.enable();
                hideButton.disable();
                hide();
                break;
        }
        gridButton.setVisible(!banClassView.contains(GClassViewType.GRID));
        panelButton.setVisible(!banClassView.contains(GClassViewType.PANEL));
        hideButton.setVisible(!banClassView.contains(GClassViewType.HIDE));

        boolean needUpdate = !classView.equals(newClassView);
        classView = newClassView;
        return needUpdate;
    }

    public void setBanClassView(List<GClassViewType> banClassView) {
        this.banClassView = banClassView;
    }

    private boolean needToBeVisible() {
        int visibles = 0;
        if (gridButton.isVisible()) visibles++;
        if (panelButton.isVisible()) visibles++;
        if (hideButton.isVisible()) visibles++;
        return visibles > 1;
    }

    public void addToToolbar(GToolbarPanel toolbar) {
        if (needToBeVisible())
            toolbar.addComponent(this);
    }

    public abstract void hide();
    public abstract void show();
}
