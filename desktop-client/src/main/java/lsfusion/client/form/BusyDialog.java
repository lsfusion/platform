package lsfusion.client.form;

import lsfusion.base.ProgressBar;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static lsfusion.client.ClientResourceBundle.getString;

class BusyDialog extends JDialog {

    private Timer longActionTimer;
    private JButton btnCopy;
    private JButton btnExit;
    private JButton btnReconnect;
    private JLabel statusMessage;
    private JPanel topPanel;
    private boolean indeterminateTopPanel = true;
    private Style defaultStyle;
    private Style highLightStyle;
    private JPanel stackPanel;
    private JScrollPane scrollPane;
    private Object[] prevLines;
    static boolean devMode = Main.configurationAccessAllowed;

    public BusyDialog(Window parent, boolean modal) {
        super(parent, getString("form.wait"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        statusMessage = new JLabel(getString("form.loading"));
        JPanel messagePanel = new JPanel();
        messagePanel.add(statusMessage);

        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JPanel messageProgressPanel = createIndeterminateTopPanel();
        topPanel.add(messageProgressPanel);
        if(devMode)
            topPanel.setMaximumSize(new Dimension((int) topPanel.getPreferredSize().getWidth(), (int) (new JProgressBar().getPreferredSize().getHeight() * 4)));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(topPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        if (devMode) {

            StyleContext styleContext = new StyleContext();
            defaultStyle = styleContext.addStyle("default", null);
            highLightStyle = styleContext.addStyle("highlight", null);
            StyleConstants.setBackground(highLightStyle, new Color(230, 230, 250));

            stackPanel = new JPanel();
            stackPanel.setBackground(null);
            stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.Y_AXIS));
            stackPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            scrollPane = new JScrollPane(stackPanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            contentPane.add(scrollPane);

            btnCopy = new JButton(getString("form.loading.copy"));
            buttonPanel.add(btnCopy);
        }

        btnExit = new JButton(getString("form.loading.exit"));
        btnExit.setEnabled(false);
        buttonPanel.add(btnExit);
        btnReconnect = new JButton(getString("form.loading.reconnect"));
        btnReconnect.setEnabled(false);
        buttonPanel.add(btnReconnect);
        buttonPanel.setMaximumSize(new Dimension((int) buttonPanel.getPreferredSize().getWidth(), (int) (btnExit.getPreferredSize().getHeight() * 4)));
        contentPane.add(buttonPanel);

        pack();

        initUIHandlers(this);

        setAlwaysOnTop(false);

        setModal(modal);

        int screenWidth = Main.frame.getRootPane().getWidth();
        if (devMode) {
            FontMetrics fm = stackPanel.getFontMetrics(stackPanel.getFont());
            stackPanel.setMinimumSize(new Dimension(stackPanel.getWidth(), fm.getHeight() * 20)); //20 lines
            setMinimumSize(new Dimension((int) (screenWidth * 0.50), fm.getHeight() * 27));
        } else {
            setMinimumSize(new Dimension((int) (screenWidth * 0.30), (int) getMinimumSize().getHeight()));
            setMaximumSize(new Dimension((int) (screenWidth * 0.50), (int) getMaximumSize().getHeight()));
            setResizable(false);
        }

        setLocationRelativeTo(parent);

        setAutoRequestFocus(false);
        final Component focusOwner = Main.frame.getFocusOwner();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                lockFrame();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                //unlockFrame();
                if (focusOwner != null)
                    focusOwner.requestFocusInWindow();
                if(longActionTimer != null)
                    longActionTimer.stop();
            }
        });
    }

    public void lockFrame() {
        if (Main.frame != null)
            Main.frame.setLocked(true);
    }

    public void unlockFrame() {
        if (Main.frame != null)
            Main.frame.setLocked(false);
    }

    private void initUIHandlers(final BusyDialog dialog) {
        if(devMode) {
            btnCopy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyToClipboard();
                }
            });
        }
        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                Main.shutdown();
            }
        });
        btnReconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                Main.reconnect();
            }
        });

        longActionTimer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnExit.setEnabled(true);
                btnReconnect.setEnabled(true);
            }
        });
        longActionTimer.start();
    }

    public void updateBusyDialog(List<Object> input) {
        if(devMode)
            setStackMessage(input);
        else
            updateTopPanel(input);
    }

    public void setStackMessage(List<Object> input) {

        Object[] lines = input.toArray();
        if (prevLines == null)
            prevLines = new Object[lines.length];

        boolean changed = false;

        int index = 0;
        for (int i = 0; i < lines.length; i++) {
            Object prevLine = prevLines.length > i ? prevLines[i] : null;
            Object line = lines[i];
            if (prevLine == null || !prevLine.equals(line))
                changed = true;
            if (line instanceof ProgressBar) {
                JPanel extraProgressBarPanel = createProgressBar((ProgressBar) line, changed);
                if (changed)
                    extraProgressBarPanel.setBackground(new Color(230, 230, 250));
                stackPanel.add(extraProgressBarPanel, index++);
            } else {
                stackPanel.add(createLine((String) line, changed), index++);
            }

        }
        //вместо удаления всех строк, что приводит к resize и дёрганью JPanel, мы сначала добавляем новые в начало, а потом удаляем старые с конца
        while(stackPanel.getComponentCount() > index)
            stackPanel.remove(index);

        prevLines = lines;
        stackPanel.add(Box.createVerticalGlue()); //to prevent stretch
        scrollPane.validate();
        scrollPane.repaint();
    }

    public JPanel createIndeterminateTopPanel() {
        JPanel messageProgressPanel = new JPanel(new BorderLayout());

        JPanel messagePanel = new JPanel();
        messagePanel.add(new JLabel(getString("form.loading")));
        messageProgressPanel.add(messagePanel, BorderLayout.NORTH);

        JPanel progressPanel = new JPanel();
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        progressPanel.add(progressBar);
        messageProgressPanel.add(progressPanel, BorderLayout.SOUTH);
        return messageProgressPanel;
    }


    public void updateTopPanel(List<Object> input) {

        Object[] lines = input.toArray();
        List<JPanel> panels = new ArrayList<>();
        for (Object line : lines) {
            if (line instanceof ProgressBar) {

                JPanel messageProgressPanel = new JPanel(new GridLayout(2, 1));

                JPanel messagePanel = new JPanel();
                messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
                messagePanel.add(new JLabel(((ProgressBar) line).message + ": "));
                JProgressBar progressBar = new JProgressBar(0, ((ProgressBar) line).total);
                progressBar.setValue(((ProgressBar) line).progress);
                messagePanel.add(progressBar);
                messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                messageProgressPanel.add(messagePanel);

                if (((ProgressBar) line).params != null) {
                    JLabel paramsLabel = new JLabel(((ProgressBar) line).params);
                    paramsLabel.setPreferredSize(new Dimension((int) messagePanel.getPreferredSize().getWidth(), (int) paramsLabel.getPreferredSize().getHeight()));
                    messageProgressPanel.add(paramsLabel);
                }
                messageProgressPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
                panels.add(messageProgressPanel);
            }

        }
        if(panels.isEmpty()) {
            if(!indeterminateTopPanel) {
                topPanel.removeAll();
                topPanel.add(createIndeterminateTopPanel());
                indeterminateTopPanel = true;
            }
        } else {
            topPanel.removeAll();
            for(JPanel panel : panels)
                topPanel.add(panel);
            indeterminateTopPanel = false;
        }
        pack();

        topPanel.validate();
        topPanel.repaint();

    }

    private JTextPane createLine(String text, boolean changed) {
        JTextPane textPane = new JTextPane();
        textPane.setBackground(null);
        textPane.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); //hack to not to scroll to the bottom (because of running with invokeLater)
        try {
            textPane.getDocument().insertString(0, splitToLines(text), changed ? highLightStyle : defaultStyle);
        } catch (BadLocationException ignored) {
        }
        textPane.setMaximumSize(textPane.getPreferredSize()); //to prevent stretch
        textPane.setMargin(new Insets(0, 0, 0, 0));
        textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return textPane;
    }

    private JPanel createProgressBar(ProgressBar progressBarLine, boolean changed) {
        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        progressBarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextPane textPane = createLine(progressBarLine.message, changed);
        progressBarPanel.add(textPane);

        JProgressBar progressBar = new JProgressBar(0, progressBarLine.total);
        progressBar.setValue(progressBarLine.progress);
        progressBarPanel.add(progressBar);

        progressBarPanel.setMaximumSize(new Dimension((int) progressBarPanel.getPreferredSize().getWidth(), (int) progressBar.getPreferredSize().getHeight()));
        return progressBarPanel;
    }

    private String splitToLines(String text) {
        FontMetrics fm = stackPanel.getFontMetrics(stackPanel.getFont());
        int maxWidth = scrollPane.getWidth() - 40;
        String[] words = text.split(" ");
        String result = "";
        String line = "";
        for(String word : words) {
            if(fm.stringWidth(line + " " + word) > maxWidth) {
                result += line + "\n";
                line = word;
            }
            else
                line += " " + word;
        }
        if(!line.isEmpty())
            result += line;
        return result;
    }

    private void copyToClipboard() {
        String stackMessageText = "";
        for(Component component : stackPanel.getComponents()) {
            if(component instanceof JTextPane)
                stackMessageText += ((JTextPane) component).getText() + "\n";
        }
        if(!stackMessageText.isEmpty())
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(stackMessageText), null);
    }
}