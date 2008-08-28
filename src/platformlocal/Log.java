package platformlocal;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Created by IntelliJ IDEA.
 * User: NewUser
 * Date: 28.08.2008
 * Time: 11:05:03
 * To change this template use File | Settings | File Templates.
 */
public final class Log {

    private final static String MSG_DELIMITER = "-----------\n";

    private static String text = "";

    public static void print(String itext) {

        text += itext;
        
        view.stateChanged();
    }

    public static void println(String itext) {
        print(itext + '\n');
    }

    public static void printmsg(String itext) {
        println(MSG_DELIMITER + itext);
    }

    private final static LogView view  = new LogView();

    public static JPanel getPanel() { return view; };

    private static class LogView extends JPanel {

        private JScrollPane pane;
        private JTextArea view;

        public LogView() {

            setLayout(new BorderLayout());

            view = new JTextArea();
            view.setEditable(false);

            JTextField fontGetter = new JTextField();
            view.setFont(fontGetter.getFont());
            
            pane = new JScrollPane(view);

            add(pane, BorderLayout.CENTER);

        }

        public void stateChanged() {
            view.setText(text);
        }
    }

}


