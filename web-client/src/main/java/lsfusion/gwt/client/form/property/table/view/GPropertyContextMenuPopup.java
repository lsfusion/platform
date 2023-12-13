package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyContextMenuPopup {
    public interface ItemSelectionListener {
        void onMenuItemSelected(String actionSID);
    }

    public static JavaScriptObject show(GPropertyDraw property, Element element, final ItemSelectionListener selectionListener) {
        if (property == null) {
            return null;
        }

        LinkedHashMap<String, String> contextMenuItems = property.getContextMenuItems();
        if (contextMenuItems == null || contextMenuItems.isEmpty()) {
            return null;
        }

        final Result<JavaScriptObject> popup = new Result<>();

        final MenuBar menuBar = new MenuBar(true);
        for (final Map.Entry<String, String> item : contextMenuItems.entrySet()) {
            final String actionSID = item.getKey();
            MenuItem menuItem = new MenuItem(ensureMenuItemCaption(item.getValue()), () -> {
                GwtClientUtils.hideTippyPopup(popup.result);
                selectionListener.onMenuItemSelected(actionSID);
            }) {
                @Override
                protected void setSelectionStyle(boolean selected) {
                    if(selected) {
                        addStyleName("context-menu-item-selected");
                    } else {
                        removeStyleName("context-menu-item-selected");
                    }
                }
            };
            menuItem.setStyleName("context-menu-item");

            menuBar.addItem(menuItem);
        }

        popup.result = GwtClientUtils.showTippyPopup(element, menuBar);
        return popup.result;
    }
    
    private static String ensureMenuItemCaption(String caption) {
        return !GwtSharedUtils.isRedundantString(caption) ? caption : EscapeUtils.UNICODE_NBSP; 
    }
}
