package skolkovo.gwt.base.client.ui;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import skolkovo.gwt.base.client.BaseFrame;

public class ToolbarPanel extends Composite {
    public @UiConstructor ToolbarPanel(boolean showProfileButton) {
        HorizontalPanel mainPane = new HorizontalPanel();
        mainPane.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        mainPane.setWidth("100%");

        if (showProfileButton) {
            String profileUrl = BaseFrame.getPageUrlPreservingParameters("expertProfile.html");

            HorizontalPanel userBtnPanel = new HorizontalPanel();
            userBtnPanel.setWidth("100%");
            userBtnPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            userBtnPanel.setSpacing(5);
            userBtnPanel.add(new HTML("<a href=\"" + profileUrl + "\" title=\"" + BaseFrame.baseMessages.showProfile() + "\"><img src=\"images/user.png\"/></a>"));
            mainPane.add(userBtnPanel);
        }

        HorizontalPanel localeBtnPanel = new HorizontalPanel();
        localeBtnPanel.setSpacing(5);
        for (final String localeName : LocaleInfo.getAvailableLocaleNames()) {
            // пропускаем GWT-шную дефолтную локаль
            if ("default".equals(localeName)) {
                continue;
            }
            localeBtnPanel.add(new HTML("<a href=\"" + BaseFrame.getPageUrlPreservingParameters("locale", localeName) + "\">" +
                                        "  <img src=\"images/" + localeName + ".png\"/></a>"));
        }
        mainPane.add(localeBtnPanel);

        initWidget(mainPane);
    }
}