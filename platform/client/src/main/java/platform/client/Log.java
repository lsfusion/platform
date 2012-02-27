package platform.client;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
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

    private static void provideSuccessFeedback() {
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
        provideSuccessFeedback();
    }

    public static void error(String message) {
        printFailedMessage(message, "");
    }

    static JTextArea errorText;
    static JDialog dialog;
    static JScrollPane sPane;
    static JOptionPane optionPane;
    static JPanel line;
    static JPanel south;

    public static void printFailedMessage(String message, String trace) {
        printmsg(message);

        provideErrorFeedback();

        JPanel panel = new JPanel();
        BorderLayout layout = new BorderLayout(10, 10);
        panel.setLayout(layout);

        StringBuilder htmlMessage = new StringBuilder("<html>");
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch == '\n') {
                htmlMessage.append("<br>");
            } else {
                htmlMessage.append(ch);
            }
        }
        htmlMessage.append("</html>");

        JLabel text = new JLabel(htmlMessage.toString());
        panel.add(text, BorderLayout.CENTER);

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
                if (dialog.isVisible() && value.equals("OK")) {
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
