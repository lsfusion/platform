package platform.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;

public final class Log {

    private static String text = "";

    public static void print(String itext) {

        text += itext;
        out.stateChanged();
    }

    public static void println(String itext) {
        print(itext + '\n');
    }

    public static void printmsg(String itext) {
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


    private final static LogView out  = new LogView();

    public static JPanel getPanel() { return out; };

    public static void printSuccessMessage(String message) {
        printmsg(message);
        out.setTemporaryBackground(Color.green);
    }

    public static void printFailedMessage(String message) {

        printmsg(message);
        
        // пока таким образом определим есть ли он на экране
        if (out.getTopLevelAncestor() != null) {
            out.setTemporaryBackground(Color.red);
            out.provideErrorFeedback();
        } else {
            JOptionPane.showMessageDialog(null, message, "LS Fusion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class LogView extends JPanel {

        private LogTextArea view;
        private JLabel info;

        public LogView() {

            setLayout(new BorderLayout());

            view = new LogTextArea();
            JScrollPane pane = new JScrollPane(view);

            add(pane, BorderLayout.CENTER);

            info = new JLabel();
            add(info, BorderLayout.PAGE_END);

            stateChanged();
        }

        public void stateChanged() {

            view.setText(text);

//            info.setText("Bytes received : " + bytesReceived);
        }

        Timer backgroundTimer;

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


