package lsfusion.client.base.log;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.MainController;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static lsfusion.client.base.view.SwingDefaults.getPanelBackground;

class LogPanel extends JPanel {
    private static final int HIDE_DELAY = 3500;
    
    private final LogTextArea logArea;
    private final JLabel info;
    private final Consumer<Boolean> visibilityConsumer;

    public LogPanel(Consumer<Boolean> visibilityConsumer) {
        this.visibilityConsumer = visibilityConsumer;
        
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

            if (MainController.enableShowingRecentlyLogMessages) {
                visibilityConsumer.accept(true);
                SwingUtils.stopSingleAction("logSetInvisible", false);

                SwingUtils.invokeLaterSingleAction("logSetInvisible",
                        e -> visibilityConsumer.accept(false),
                        HIDE_DELAY);
            }
        }
    }

    public void setTemporaryBackground(Color color) {
        SwingUtils.stopSingleAction("logSetOldBackground", true);

        logArea.setBackground(color);

        SwingUtils.invokeLaterSingleAction("logSetOldBackground",
                e -> logArea.setBackground(getPanelBackground()),
                HIDE_DELAY);
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
