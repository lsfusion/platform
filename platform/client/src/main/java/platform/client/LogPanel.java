package platform.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class LogPanel extends JPanel {
    private final LogTextArea logArea;
    private final JLabel info;

    public LogPanel() {
        setLayout(new BorderLayout());

        logArea = new LogTextArea();
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(logArea);

        add(pane, BorderLayout.CENTER);

        info = new JLabel();
        add(info, BorderLayout.PAGE_END);
    }

    public void updateText(String newText) {
        logArea.setText(newText);
        if (!newText.isEmpty()) {
            logArea.setCaretPosition(newText.length() - 1);
        }
    }

    public void setTemporaryBackground(Color color) {
        SwingUtils.stopSingleAction("logSetOldBackground", true);

        final Color oldBackground = logArea.getBackground();
        logArea.setBackground(color);

        SwingUtils.invokeLaterSingleAction("logSetOldBackground", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logArea.setBackground(oldBackground);
            }
        }, 10000);
    }

    public void provideErrorFeedback() {
        UIManager.getLookAndFeel().provideErrorFeedback(logArea);
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
