package lsfusion.client;

import lsfusion.base.ExceptionUtils;
import lsfusion.client.rmi.ConnectionLostManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public final class Log {
    private static String text = "";

    private static WeakReference<LogPanel> logPanelRef = new WeakReference<LogPanel>(null);

    public static JPanel recreateLogPanel() {
        LogPanel logPanel = new LogPanel();

        logPanelRef = new WeakReference<LogPanel>(logPanel);
        text = "";

        return logPanel;
    }

    private static LogPanel getLogPanel() {
        LogPanel logPanel = logPanelRef.get();
        // пока таким образом определим есть ли он на экране
        if (logPanel != null && logPanel.getTopLevelAncestor() != null) {
            return logPanel;
        }

        return null;
    }

    private static void print(String itext) {
        text += itext;
        stateChanged();
    }

    private static void println(String itext) {
        print(itext + '\n');
    }

    private static void printmsg(String itext) {
        println(getMsgHeader() + itext + getMsgFooter());
    }

    private static String getMsgHeader() {
        return "--- " + DateFormat.getInstance().format(new Date(System.currentTimeMillis())) + " ---\n";
    }

    private static String getMsgFooter() {
        return "";
    }

    private static void stateChanged() {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.updateText(text);
        }
    }

    private static void provideSuccessFeedback(String message) {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.setTemporaryBackground(Color.green);
        }
    }

    private static void provideErrorFeedback() {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.setTemporaryBackground(Color.red);
            logPanel.provideErrorFeedback();
        }
    }

    public static void message(String message) {
        printmsg(message);
        provideSuccessFeedback(message);
    }

    public static void error(String message) {
        error(message, null, null, false);
    }

    public static void error(String message, Throwable t) {
        error(message, t, false);
    }

    public static void error(String message, String trace) {
        error(message, null, null, trace, false);
    }

    public static void error(String message, List<String> titles, List<List<String>> data, boolean warning) {
        error(message, titles, data, "", warning);
    }

    public static void error(String message, Throwable t, boolean forcedShowError) {
        error(message, null, null, ExceptionUtils.getStackTraceString(t), forcedShowError);
    }

    private static void error(String message, List<String> titles, List<List<String>> data, String trace, boolean warning) {
        error(message, titles, data, trace, false, warning);
    }

    private static void error(String message, List<String> titles, List<List<String>> data, String trace, boolean forcedShowError, boolean warning) {
        if (!forcedShowError && ConnectionLostManager.isConnectionLost()) {
            return;
        }

        SwingUtils.assertDispatchThread();

        printmsg(message);

        provideErrorFeedback();

        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.add(new JLabel(toHtml(message)), BorderLayout.NORTH);
        if (data != null) {
            messagePanel.add(new JScrollPane(createDataTable(titles, data)), BorderLayout.CENTER);
        }

        JPanel line = new JPanel();
        line.setPreferredSize(new Dimension(10, 2));
        line.setBackground(Color.GRAY);

        JTextArea taErrorText = new JTextArea(trace, 7, 60);
        taErrorText.setFont(new Font("Tahoma", Font.PLAIN, 12));
        taErrorText.setForeground(Color.RED);

        final JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setVisible(false);
        south.add(line);
        south.add(new JLabel(" "));
        south.add(new JScrollPane(taErrorText));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(messagePanel, BorderLayout.CENTER);
        mainPanel.add(south, BorderLayout.SOUTH);

        String opt[];
        if (trace.length() > 0) {
            opt = new String[]{"OK", getString("client.more")};
        } else {
            opt = new String[]{"OK"};
        }
        final JOptionPane optionPane = new JOptionPane(mainPanel, warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE,
                                     JOptionPane.YES_NO_OPTION,
                                     null,
                                     opt,
                                     "OK");

        final JDialog dialog = new JDialog(Main.frame, Main.getMainTitle(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(optionPane);
        dialog.pack();

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                Object value = optionPane.getValue();
                if (dialog.isVisible() && (value.equals("OK") || value.equals(-1))) {
                    dialog.dispose();
                } else if (value.equals(getString("client.more"))) {
                    south.setVisible(!south.isVisible());
                    dialog.pack();
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                }
            }
        });


        //центрируем на экране
        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }

    private static Component createDataTable(List<String> titles, List<List<String>> data) {
        int size = data.size();
        Object columnNames[] = titles.toArray();
        Object dataArray[][] = new Object[size][];
        int i = 0;
        for (List<String> dataRow : data) {
            dataArray[i] = dataRow.toArray();
            i++;
        }
        JTable table = new JTable(dataArray, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setPreferredScrollableViewportSize(
                new Dimension(
                        table.getPreferredScrollableViewportSize().width,
                        dataArray.length * table.getRowHeight()
                ));

        Caret caret = new DefaultCaret()
        {
            public void focusGained(FocusEvent e)
            {
                setVisible(true);
                setSelectionVisible(true);
            }
        };
        caret.setBlinkRate( UIManager.getInt("TextField.caretBlinkRate") );

        JTextField textField = new JTextField();
        textField.setEditable(false);
        textField.setCaret(caret);
        textField.setBorder(new LineBorder(Color.BLACK));

        DefaultCellEditor dce = new DefaultCellEditor(textField);
        for(int j = 0; j < table.getColumnModel().getColumnCount(); j++) {
            table.getColumnModel().getColumn(j).setCellEditor(dce); 
        }
       
        table.setFocusable(true);
        return table;
    }

    private static String toHtml(String message) {
        StringBuilder htmlMessage = new StringBuilder("<html><font size=+1>");
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch == '\n') {
                htmlMessage.append("<br>");
            } else {
                htmlMessage.append(ch);
            }
        }
        htmlMessage.append("</font></html>");
        return htmlMessage.toString();
    }
}
