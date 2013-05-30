package platform.gwt.sgwtbase.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

import java.util.LinkedHashMap;

import static platform.gwt.base.client.GwtClientUtils.*;

public class ToolStripPanel extends ToolStrip {
    private Label lbTitle;

    public ToolStripPanel(String title) {
        this(null, title);
    }

    public ToolStripPanel(String logoUrl, String title) {
        this(logoUrl, title, true);
    }

    public ToolStripPanel(String logoUrl, String title, boolean showLogoutButton) {
        Page.setAppImgDir(GWT.getModuleBaseURL() + "images/");

        setHeight(33);
        setWidth100();

        addSpacer(6);
        ImgButton homeButton = new ImgButton();

        if (logoUrl == null) {
            logoUrl = "logo_toolbar.png";
        }
        homeButton.setSrc(logoUrl);

        homeButton.setWidth(24);
        homeButton.setHeight(24);
        homeButton.setShowRollOver(false);
        homeButton.setShowDownIcon(false);
        homeButton.setShowDown(false);
        homeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
//                com.google.gwt.user.client.Window.open("http://i-gorod.../", "newwindow", null);
            }
        });
        addMember(homeButton);
        addSpacer(6);

        lbTitle = new Label(title);
        lbTitle.setStyleName("logoTitle");
        lbTitle.setWidth(300);
        addMember(lbTitle);

        addFill();

        if (!GWT.isScript()) {
            ToolStripButton devConsoleButton = new ToolStripButton();
            devConsoleButton.setTitle("Developer Console");
            devConsoleButton.setIcon("bug.png");
            devConsoleButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    SC.showConsole();
                }
            });
            addButton(devConsoleButton);

            addSeparator();
        }

        addMember(createLocaleChooser());

        if (showLogoutButton) {
            addSeparator();

            ToolStripButton logoutBtn = new ToolStripButton();
            logoutBtn.setTitle(baseMessages.logout());
            logoutBtn.setIcon("door.png");
            logoutBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    logout();
                }
            });

            addMember(logoutBtn);
        }

        addSpacer(6);
    }

    private static final String locales[] = {"ru", "en"};
    private static final String localesDescriptions[] = {
            baseMessages.localeRu(),
            baseMessages.localeEn()
    };

    private Canvas createLocaleChooser() {
        SelectItem selectItem = new SelectItem();
        selectItem.setHeight(21);
        selectItem.setWidth(130);
        selectItem.setShowTitle(false);

        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < locales.length; ++i) {
            valueMap.put(locales[i], localesDescriptions[i]);
        }

        selectItem.setValueMap(valueMap);
        selectItem.setDefaultValue(LocaleInfo.getCurrentLocale().getLocaleName());

        selectItem.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
//                Cookies.setCookie(skinCookieName, (String) event.getValue());
//                com.google.gwt.user.client.Window.Location.reload();

                Window.open(getPageUrlPreservingParameters("locale", (String) event.getValue()), "_self", "");
            }
        });

        DynamicForm form = new DynamicForm();
        form.setNumCols(1);
        form.setFields(selectItem);

        return form;
    }

    public void setTitle(String title) {
        lbTitle.setContents(title);
        lbTitle.redraw();
    }
}
