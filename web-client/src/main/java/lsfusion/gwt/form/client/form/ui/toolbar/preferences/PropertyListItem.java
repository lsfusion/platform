package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class PropertyListItem {
    public GPropertyDraw property;
    Boolean inGrid; // false - panel, null - hidden through showIf

    public PropertyListItem(GPropertyDraw property, Boolean inGrid) {
        this.property = property;
        this.inGrid = inGrid;
    }

    @Override
    public String toString() {
        String result = property.getNotEmptyCaption();
        if (inGrid == null) {
            result += " (не отображается)";
        } else if (!inGrid) {
            result += " (в панели)";
        }
        return result;
    }
}
