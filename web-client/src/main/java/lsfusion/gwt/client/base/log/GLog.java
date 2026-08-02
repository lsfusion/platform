package lsfusion.gwt.client.base.log;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;

public final class GLog {
    private static LogPanel logPanel;
    public static boolean isLogPanelVisible;

    // the one place the log window's view is chosen, and the only place that knows the window: WINDOW log CUSTOM names
    // a React component, and then that component draws the window instead of the standard panel. The standard panel is
    // not built at all in that case - a message goes to whichever panel was chosen here, and to nothing else.
    // Not in the mobile layout though: a component draws a window in the desktop web layout only, which is the rule for
    // every window - a navigator window's component is already never drawn there, since the mobile navigator view draws
    // that window instead - and the mobile layout has no log window at all to draw
    public static Widget createLogPanel(GAbstractWindow window, Runnable togglePinMode) {
        // ...and not for a window HIDE WINDOW has taken away: it is never shown, so a component drawn into it would
        // never be seen, and the messages it collects would be unreachable. Said once, because an application that
        // hides the window and gives it a component has asked for two things that cancel each other out
        if (window.react && !window.visible)
            GwtClientUtils.logLsfViewError("'" + window.custom + "' is not drawn: the window '" + window.canonicalName
                    + "' is hidden, so nothing it draws would be seen");

        logPanel = window.react && window.visible && !MainFrame.mobile ? new ReactLogPanel(window.custom, togglePinMode) : new GLogPanel(togglePinMode);
        isLogPanelVisible = window.visible;
        return logPanel;
    }

    public static Widget toPrintMessage(String message, StaticImage image, ArrayList<ArrayList<String>> data, ArrayList<String> titles) {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();

        panel.add(EscapeUtils.toHTML(message, image));

        if (!data.isEmpty()) {
            FlexTable table = new FlexTable();
            GwtClientUtils.addClassName(table, "table");
            table.setCellSpacing(0);
            table.setBorderWidth(1);
            table.setWidth("100%");

            for (int i = 0; i < titles.size(); i++) {
                Widget titleWidget = EscapeUtils.toHTML(titles.get(i));
//                "<b style=\"font-size: 8pt\">" + EscapeUtils.toHtml(titles.get(i)) + "</b>"
                table.setWidget(0, i, titleWidget);
            }

            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    Widget dataWidget = EscapeUtils.toHTML(data.get(i).get(j));
//                    "<div style=\"font-size: 8pt\">" + s + "</div>"
                    table.setWidget(i + 1, j, dataWidget);
                }
            }

            panel.add(table);
        }
        return panel;
    }

    public static void message(Widget message, String caption, boolean failed) {
        logPanel.printMessage(message, caption, failed);
    }

    public static native void showFocusNotification(String message, String caption, boolean ifNotFocused)/*-{
        return $wnd.showFocusNotification(message, caption, ifNotFocused);
    }-*/;
}
