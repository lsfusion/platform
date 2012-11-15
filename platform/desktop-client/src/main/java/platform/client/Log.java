package platform.client;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import static platform.client.ClientResourceBundle.getString;

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
        } else if (!Main.module.isFull()) {
            JOptionPane.showMessageDialog(SwingUtils.getActiveWindow(), message, Main.getMainTitle(), JOptionPane.INFORMATION_MESSAGE);
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
        error(message, null, null);
    }

    public static void error(String message, ArrayList<String> titles, ArrayList<ArrayList<String>> data) {
        printFailedMessage(message, titles, data, "");
    }

    static JTextArea errorText;
    static JDialog dialog;
    static JScrollPane sPane;
    static JOptionPane optionPane;
    static JPanel line;
    static JPanel south;
    static JTable table;

    public static void printFailedMessage(String message, String trace) {
        printFailedMessage(message, null, null, trace);
    }

    public static void printFailedMessage(String message, ArrayList<String> titles, ArrayList<ArrayList<String>> data, String trace) {
        printmsg(message);

        provideErrorFeedback();

        JPanel panel = new JPanel();
        BorderLayout layout = new BorderLayout(10, 10);
        panel.setLayout(layout);

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout(10, 10));

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

        JLabel text = new JLabel(htmlMessage.toString());
        messagePanel.add(text, BorderLayout.NORTH);

        if (data != null) {
            int size = data.size();
            Object columnNames[] = titles.toArray();
            Object dataArray[][] = new Object[size][];
            int i = 0;
            for (ArrayList<String> dataRow : data) {
                dataArray[i] = dataRow.toArray();
                i++;
            }
            table = new JTable(dataArray, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.setPreferredScrollableViewportSize(
                    new Dimension(
                            table.getPreferredScrollableViewportSize().width,
                            dataArray.length * table.getRowHeight()
                    ));
            table.setFocusable(false);
            JScrollPane scrollPane = new JScrollPane(table);
            messagePanel.add(scrollPane, BorderLayout.CENTER);
        }

        panel.add(messagePanel, BorderLayout.CENTER);

        south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setVisible(false);

        line = new JPanel();
        south.add(line);
        south.add(new JLabel(" "));
        line.setPreferredSize(new Dimension(10, 2));
        line.setBackground(Color.GRAY);

        errorText = new JTextArea(trace, 7, 60);
        errorText.setFont(new Font("Tahoma", Font.PLAIN, 12));
        errorText.setForeground(Color.RED);

        sPane = new JScrollPane(errorText);
        south.add(sPane);
        panel.add(south, BorderLayout.SOUTH);

        String opt[];
        if (trace.length() > 0) {
            opt = new String[]{"OK", getString("client.more")};
        } else {
            opt = new String[]{"OK"};
        }
        optionPane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE,
                                     JOptionPane.YES_NO_OPTION,
                                     null,
                                     opt,
                                     "OK");

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

        dialog = new JDialog(Main.frame, Main.getMainTitle(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(optionPane);
        dialog.pack();

        //центрируем на экране
        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }
}
