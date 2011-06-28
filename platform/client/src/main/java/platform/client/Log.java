package platform.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;

public final class Log {

    private static String text = "";

    private static void print(String itext) {

        text += itext;
        out.stateChanged();
    }

    private static void println(String itext) {
        print(itext + '\n');
    }

    private static void printmsg(String itext) {
        println(getMsgHeader() + itext + getMsgFooter());
    }

    private static int bytesReceived = 0;

    public static void incrementBytesReceived(int cnt) {

        bytesReceived += cnt;
        out.stateChanged();
    }

    private static String getMsgHeader() {
        return "--- " + DateFormat.getInstance().format(new Date(System.currentTimeMillis())) + " ---\n";
    }

    private static String getMsgFooter() {
        return "";
    }


    private final static LogView out = new LogView();

    public static JPanel getPanel() {
        return out;
    }

    public static void printSuccessMessage(String message) {
        printmsg(message);
        // пока таким образом определим есть ли он на экране
        if (out.getTopLevelAncestor() != null) {
            out.setTemporaryBackground(Color.green);
        } else {
            JOptionPane.showMessageDialog(Main.frame, message, Main.getMainTitle(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void printFailedMessage(String message) {
        printFailedMessage(message, "", null);
    }

    static JTextArea errorText;
    static JDialog dialog;
    static JScrollPane sPane;
    static JOptionPane optionPane;
    static JPanel line;
    static JPanel south;

    public static void printFailedMessage(String message, String trace, Component parentComponent) {

        printmsg(message);

        // пока таким образом определим есть ли он на экране
        if (out.getTopLevelAncestor() != null) {
            out.setTemporaryBackground(Color.red);
            out.provideErrorFeedback();
        }

        // ошибки всегда идут на экран
        //JOptionPane.showMessageDialog(parentComponent, message, "LS Fusion", JOptionPane.ERROR_MESSAGE);
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
        //panel.add(new JLabel(" "));

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
            opt = new String[]{"OK", "Подробнее"};
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
                } else if (value.equals("Подробнее")) {
                    south.setVisible(!south.isVisible());
                    dialog.pack();
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                }
            }
        });

        dialog = new JDialog(Main.frame, Main.getMainTitle(), true);
        dialog.setContentPane(optionPane);
        dialog.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = dialog.getWidth();
        int height = dialog.getHeight();
        dialog.setLocation((int) (screenSize.getWidth() - width) / 2, (int) (screenSize.getHeight() - height) / 2);

        dialog.setVisible(true);
    }

    private static class LogView extends JPanel {

        private final LogTextArea view;
        private final JLabel info;

        public LogView() {

            setLayout(new BorderLayout());

            view = new LogTextArea();
            view.setLineWrap(true);
            view.setWrapStyleWord(true);
            JScrollPane pane = new JScrollPane(view);

            add(pane, BorderLayout.CENTER);

            info = new JLabel();
            add(info, BorderLayout.PAGE_END);

            stateChanged();
        }

        public void stateChanged() {

            view.setText(text);
            if (!text.isEmpty())
                view.setCaretPosition(text.length() - 1);

//            info.setText("Bytes received : " + bytesReceived);
        }

        public void setTemporaryBackground(Color color) {

            SwingUtils.stopSingleAction("logSetOldBackground", true);

            final Color oldBackground = view.getBackground();
            view.setBackground(color);

            SwingUtils.invokeLaterSingleAction("logSetOldBackground", new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    view.setBackground(oldBackground);
                }
            }, 10000);

        }

        public void provideErrorFeedback() {
            UIManager.getLookAndFeel().provideErrorFeedback(view);
        }

        class LogTextArea extends JTextArea {

            public LogTextArea() {
                super();

                setEditable(false);
            }

            public void updateUI() {
                super.updateUI();

                JTextField fontGetter = new JTextField();
                setFont(fontGetter.getFont());
            }
        }
    }

}


