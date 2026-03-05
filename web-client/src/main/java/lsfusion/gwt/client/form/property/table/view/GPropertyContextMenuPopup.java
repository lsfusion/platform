package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyContextMenuPopup {
    public interface ItemSelectionListener {
        void onMenuItemSelected(String actionSID);
    }

    public static void show(PopupOwner popupOwner, GPropertyDraw property, final ItemSelectionListener selectionListener) {
        if (property == null) {
            return;
        }

        LinkedHashMap<String, String> contextMenuItems = property.getContextMenuItems();
        if (contextMenuItems == null || contextMenuItems.isEmpty()) {
            return;
        }

        final Result<JavaScriptObject> popup = new Result<>();

        final MenuBar menuBar = new MenuBar(true);
        for (final Map.Entry<String, String> item : contextMenuItems.entrySet()) {
            final String actionSID = item.getKey();
            MenuItem menuItem = new MenuItem(ensureMenuItemCaption(item.getValue()), () -> {
                GwtClientUtils.hideAndDestroyTippyPopup(popup.result);
                selectionListener.onMenuItemSelected(actionSID);
            }) {
                @Override
                protected void setSelectionStyle(boolean selected) {
                    if(selected) {
                       GwtClientUtils.addClassName(this, "context-menu-item-selected");
                    } else {
                       GwtClientUtils.removeClassName(this, "context-menu-item-selected");
                    }
                }
            };
            GwtClientUtils.addClassName(menuItem, "context-menu-item");

            GPropertyDraw.ContextMenuDebugInfo debugInfo = property.contextMenuDebugInfoMap.get(actionSID);
            if(debugInfo != null) {
                TooltipManager.initTooltip(new PopupOwner(menuBar, menuItem.getElement()), new TooltipManager.TooltipHelper() {
                    @Override
                    public String getTooltip(String dynamicTooltip) {
                        return debugInfo.getTooltip(debugInfo.sid, menuItem.getText());
                    }

                    @Override
                    public String getCreationPath() {
                        return debugInfo.creationPath;
                    }

                    @Override
                    public String getPath() {
                        return debugInfo.path;
                    }
                });
            }
            menuBar.addItem(menuItem);
        }

        popup.result = GwtClientUtils.showTippyPopup(popupOwner, menuBar);
    }
    
    private static String ensureMenuItemCaption(String caption) {
        return !GwtSharedUtils.isRedundantString(caption) ? caption : EscapeUtils.UNICODE_NBSP; 
    }
}
