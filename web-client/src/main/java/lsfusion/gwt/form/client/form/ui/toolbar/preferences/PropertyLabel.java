package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;

public class PropertyLabel extends Label {
    private PropertyListItem propertyItem;

    public PropertyLabel(PropertyListItem propertyItem, boolean visibleList) {
        super();
        setWordWrap(false);
        this.propertyItem = propertyItem;

        refreshLabel(visibleList);
    }

    public PropertyListItem getPropertyItem() {
        return propertyItem;
    }
    
    public String getUserCaption(boolean ignoreDefault) {
        return propertyItem.getUserCaption(ignoreDefault);
    }

    public String getUserPattern(boolean ignoreDefault) {
        return propertyItem.getUserPattern(ignoreDefault);
    }
    
    public void setUserCaption(String userCaption) {
        propertyItem.setUserCaption(userCaption);
    }

    public void setUserPattern(String userPattern) {
        propertyItem.setUserPattern(userPattern);
    }
    
    public void refreshLabel(boolean visibleList) {
        propertyItem.setVisible(visibleList);
        setText(propertyItem.toString());
        
        Style itemStyle = getElement().getStyle();
        boolean gray = false;
        if (visibleList) {
            if (propertyItem.inGrid == null || !propertyItem.inGrid) {
                gray = true;
            }
        } else if (propertyItem.inGrid != null && !propertyItem.inGrid) { // справа не выделяем спрятанные колонки, т.к. пока никак не отличаем спрятанные настройкой от спрятанных через showIf
            gray = true;
        }
        
        if (gray) {
            itemStyle.setColor("#a7a7a7");    
        } else {
            itemStyle.setColor("black");
        }
    }
}
