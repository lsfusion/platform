package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;

public class PropertyLabel extends Label {
    private PropertyListItem propertyItem;

    public PropertyLabel(PropertyListItem propertyItem) {
        super(propertyItem.toString(), false);
        this.propertyItem = propertyItem;
        
        Style itemStyle = getElement().getStyle();
        if (propertyItem.inGrid == null || !propertyItem.inGrid) {
            itemStyle.setColor("#a7a7a7");
        } else {
            itemStyle.setColor("black");
        }
    }

    public PropertyListItem getPropertyItem() {
        return propertyItem;
    }
    
    public String getUserCaption(boolean ignoreDefault) {
        return propertyItem.getUserCaption(ignoreDefault);
    }
    
    public void setUserCaption(String userCaption) {
        propertyItem.setUserCaption(userCaption);
    }
}
