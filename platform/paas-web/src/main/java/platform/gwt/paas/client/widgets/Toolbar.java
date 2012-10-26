package platform.gwt.paas.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.event.shared.EventBus;
import com.google.inject.Inject;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.login.LogoutAuthenticatedEvent;

public class Toolbar extends ToolStrip {
    @Inject
    private EventBus eventBus;
    @Inject
    private PaasPlaceManager placeManager;
    private ImgButton loadingIndicator;

    public Toolbar() {
        setWidth100();
    }

    public ToolStripButton addToolStripButton(String iconPath, String toolTip, ClickHandler clickHandler) {
        return addToolStripButton("", iconPath, toolTip, clickHandler);
    }

    public ToolStripButton addToolStripButton(String title, String iconPath, String toolTip, ClickHandler clickHandler) {
        ToolStripButton button = new ToolStripButton(title, "toolbar/" + iconPath);
        button.setTooltip(toolTip);
        button.addClickHandler(clickHandler);

        addButton(button);

        return button;
    }

    protected void addConsoleButton() {
        if (!GWT.isScript()) {
            addToolStripButton("Developer Console", "bug.png", "Developer Console", new ClickHandler() {
                public void onClick(ClickEvent event) {
                    SC.showConsole();
                }
            });
        }
    }

    public void addLogoutButton() {
        addToolStripButton("Log out", "door.png", "Logout", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                LogoutAuthenticatedEvent.fire(eventBus);
            }
        });
    }

    public void addHomeButton() {
        addToolStripButton("", "home.png", "Go to start page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                placeManager.revealDefaultPlace();
            }
        });
    }

    public void addLoadingIndicator() {
        loadingIndicator = new ImgButton();
        loadingIndicator.setWidth(16);
        loadingIndicator.setHeight(16);
        loadingIndicator.setSrc("updating.gif");
        loadingIndicator.setShowRollOver(false);
        loadingIndicator.setShowDownIcon(false);
        loadingIndicator.setShowDown(false);
        loadingIndicator.hide();
        addMember(loadingIndicator);
    }

    public void showLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.show();
        }
    }

    public void hideLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.hide();
        }
    }
}
