package lsfusion.client.base.log;

import lsfusion.client.base.SwingUtils;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.view.SwingDefaults.getPanelBackground;

class LogPanel extends JPanel {
    private final LogTextArea logArea;
    private final JLabel info;

    public LogPanel() {
        setLayout(new BorderLayout());

        logArea = new LogTextArea();
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(logArea);
        pane.setBorder(BorderFactory.createEmptyBorder());

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

        logArea.setBackground(color);

        SwingUtils.invokeLaterSingleAction("logSetOldBackground",
                e -> logArea.setBackground(getPanelBackground()),
                10000);
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
            setBackground(getPanelBackground());
        }
    }
}
