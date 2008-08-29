package platformlocal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: NewUser
 * Date: 28.08.2008
 * Time: 11:05:03
 * To change this template use File | Settings | File Templates.
 */
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
        out.setTemporaryBackground(Color.red);
        out.provideErrorFeedback();
    }

    private static class LogView extends JPanel {

        private JScrollPane pane;
        private LogTextArea view;
        private JLabel info;

        public LogView() {

            setLayout(new BorderLayout());

            view = new LogTextArea();
            pane = new JScrollPane(view);

            add(pane, BorderLayout.CENTER);

            info = new JLabel();
            add(info, BorderLayout.PAGE_END);

            stateChanged();
        }

        public void stateChanged() {

            view.setText(text);

            info.setText("Bytes received : " + bytesReceived);
        }

        Timer backgroundTimer;

        public void setTemporaryBackground(Color color) {

            if (backgroundTimer != null) {
                ActionListener[] actions = backgroundTimer.getActionListeners();
                for (ActionListener action : actions)
                    action.actionPerformed(null);
                backgroundTimer.stop();
                backgroundTimer = null;
            }

            final Color oldBackground = view.getBackground();
            view.setBackground(color);

            backgroundTimer = new Timer(10000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    view.setBackground(oldBackground);
                }
            });
            backgroundTimer.setRepeats(false);

            backgroundTimer.start();
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


