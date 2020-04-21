package lsfusion.client.form.object.table.grid.user.design;

import lsfusion.client.form.property.ClientPropertyDraw;

import static lsfusion.client.ClientResourceBundle.getString;

public class UserPreferencesPropertyListItem {
    public ClientPropertyDraw property;
    private String userCaption;
    private String userPattern;
    public Boolean inGrid; // false - panel, null - hidden through showIf
    private boolean visibleList;

    public UserPreferencesPropertyListItem(ClientPropertyDraw property, String userCaption, String userPattern, Boolean inGrid) {
        this.property = property;
        this.userCaption = userCaption;
        this.userPattern = userPattern;
        this.inGrid = inGrid;
    }

    public String getDefaultCaption() {
        return property.getPropertyCaption();
    }

    public String getUserCaption(boolean ignoreDefault) {
        return userCaption != null ? userCaption : (ignoreDefault ? null : getDefaultCaption());
    }

    public void setUserCaption(String userCaption) {
        this.userCaption = userCaption;
    }

    public String getUserPattern(boolean ignoreDefault) {
        return userPattern != null ? userPattern : (ignoreDefault ? null : property.getFormatPattern());
    }

    public void setUserPattern(String userPattern) {
        this.userPattern = userPattern;
    }

    @Override
    public String toString() {
        String result = getUserCaption(false);
        if (inGrid == null) {
            if (visibleList) {
                result += " (" + getString("form.grid.preferences.property.not.shown") + ")";
            }
        } else if (!inGrid) {
            result += " (" + getString("form.grid.preferences.property.in.panel") + ")";
        }
        return result;
    }
    
    public void setVisibleList(boolean visibleList) {
        this.visibleList = visibleList;
    }
}