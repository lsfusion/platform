package lsfusion.gwt.client.base.log;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;

import java.util.ArrayList;

public final class GLog {
    private static GLogPanel logPanel;
    public static boolean isLogPanelVisible;

    public static Widget createLogPanel(boolean visible) {
        logPanel = new GLogPanel();
        isLogPanelVisible = visible;
        return logPanel;
    }

    public static Widget toPrintMessage(String message, StaticImage image, ArrayList<ArrayList<String>> data, ArrayList<String> titles) {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();

        panel.add(EscapeUtils.toHTML(message, image));

        if (!data.isEmpty()) {
            FlexTable table = new FlexTable();
            table.addStyleName("table");
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
