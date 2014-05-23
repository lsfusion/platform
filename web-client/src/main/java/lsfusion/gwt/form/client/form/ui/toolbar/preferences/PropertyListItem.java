package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class PropertyListItem {
    public GPropertyDraw property;
    private String userCaption;
    Boolean inGrid; // false - panel, null - hidden through showIf

    public PropertyListItem(GPropertyDraw property, String userCaption, Boolean inGrid) {
        this.property = property;
        this.userCaption = userCaption;
        this.inGrid = inGrid;
    }

    public String getUserCaption(boolean ignoreDefault) {
        return userCaption != null ? userCaption : (ignoreDefault ? null : property.getNotEmptyCaption());
    }

    public void setUserCaption(String userCaption) {
        this.userCaption = userCaption;
    }

    @Override
    public String toString() {
        String result = getUserCaption(false);
        if (inGrid == null) {
            result += " (не отображается)";
        } else if (!inGrid) {
            result += " (в панели)";
        }
        return result;
    }
}
