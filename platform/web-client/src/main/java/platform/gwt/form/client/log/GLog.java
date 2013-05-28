package platform.gwt.form.client.log;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.DialogBoxHelper;
import platform.gwt.base.client.ui.ResizableVerticalPanel;

import java.util.ArrayList;
import java.util.Date;

public final class GLog {
    private static GLogPanel logPanel;
    public static boolean isLogPanelVisible;

    public static Widget createLogPanel(boolean visible) {
        logPanel = new GLogPanel();
        isLogPanelVisible = visible;
        return logPanel;
    }

    public static void error(String message, ArrayList<ArrayList<String>> data, ArrayList<String> titles) {
        error(message);

        ResizableVerticalPanel panel = new ResizableVerticalPanel();
        HTML constraintMessage = new HTML("<h3 style=\"margin-top: 0;\">" + message + "</h3>");
        panel.add(constraintMessage);

        if (!data.isEmpty()) {
            FlexTable table = new FlexTable();
            table.setCellSpacing(0);
            table.setBorderWidth(1);
            table.setWidth("100%");

            for (int i = 0; i < titles.size(); i++) {
                table.setHTML(0, i, "<b style=\"font-size: 8pt\">" + titles.get(i) + "</b>");
            }

            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    table.setHTML(i + 1, j, "<div style=\"font-size: 8pt\">" + data.get(i).get(j) + "</div>");
                }
            }

            panel.add(table);
        }

        DialogBoxHelper.showMessageBox(true, "lsFusion", panel, null);
    }

    public static void error(String message) {
        logPanel.printError(completeMessage(message));
    }

    public static void message(String message) {
        logPanel.printMessage(completeMessage(message));
    }

    private static String completeMessage(String message) {
        return getMsgHeader() + message;
    }

    private static String getMsgHeader() {
        return "--- " + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM).format(new Date(System.currentTimeMillis())) + " ---<br/>";
    }
}
