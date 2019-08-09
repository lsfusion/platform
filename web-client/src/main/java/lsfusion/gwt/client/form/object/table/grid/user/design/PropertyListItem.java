package lsfusion.gwt.client.form.object.table.grid.user.design;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class PropertyListItem {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public GPropertyDraw property;
    private String userCaption;
    private String userPattern;
    public Boolean inGrid; // false - panel, null - hidden through showIf
    private boolean visibleList;

    public PropertyListItem(GPropertyDraw property, String userCaption, String userPattern, Boolean inGrid) {
        this.property = property;
        this.userCaption = userCaption;
        this.userPattern = userPattern;
        this.inGrid = inGrid;
    }

    public String getUserCaption(boolean ignoreDefault) {
        return userCaption != null ? userCaption : (ignoreDefault ? null : property.getNotEmptyCaption());
    }

    public String getUserPattern() {
        return userPattern;
    }

    public void setUserCaption(String userCaption) {
        this.userCaption = userCaption;
    }

    public void setUserPattern(String userPattern) {
        this.userPattern = userPattern;
    }

    public void setVisible(boolean visibleList) {
        this.visibleList = visibleList;
    }

    @Override
    public String toString() {
        String result = getUserCaption(false);
        if (inGrid == null) {
            if (visibleList) {
                result += " (" + messages.formGridPreferencesPropertyNotShown() + ")";
            }
        } else if (!inGrid) {
            result += " (" + messages.formGridPreferencesPropertyInPanel() + ")";
        }
        return result;
    }
}
