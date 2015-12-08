package lsfusion.client.form;

import lsfusion.base.ProgressBar;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

class BusyDialog extends JDialog {

    private Timer longActionTimer;
    private JButton btnCopy;
    private JButton btnExit;
    private JButton btnReconnect;
    private JButton btnCancel;
    private JButton btnInterrupt;
    private boolean indeterminateProgressBar = false;
    private Style defaultStyle;
    private Style highLightStyle;
    private JPanel stackPanel;
    private JPanel subStackPanel;
    private JScrollPane scrollPane;
    private Object[] prevLines;
    private static boolean devMode = Main.configurationAccessAllowed;
    private GridBagConstraints fieldConstraints = null;

    private boolean longAction;
    private Integer processId = null;

    public BusyDialog(Window parent, boolean modal) {
        super(parent, getString("form.wait"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        fieldConstraints = initGridBagConstraints();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        StyleContext styleContext = new StyleContext();
        defaultStyle = styleContext.addStyle("default", null);
        highLightStyle = styleContext.addStyle("highlight", null);
        StyleConstants.setBackground(highLightStyle, new Color(230, 230, 250));

        stackPanel = new JPanel();
        stackPanel.setBackground(null);
        stackPanel.setLayout(new GridBagLayout());
        stackPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        subStackPanel = new JPanel();
        subStackPanel.setLayout(new GridBagLayout());

        addGridBagComponent(createTopProgressBarPanel(true), stackPanel);

        scrollPane = new JScrollPane(stackPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentPane.add(scrollPane);

        if(devMode) {
            btnCopy = new JButton(getString("form.loading.copy"));
            buttonPanel.add(btnCopy);
        }

        btnExit = new JButton(getString("form.loading.exit"));
        btnExit.setEnabled(false);
        buttonPanel.add(btnExit);
        btnReconnect = new JButton(getString("form.loading.reconnect"));
        btnReconnect.setEnabled(false);
        buttonPanel.add(btnReconnect);

        btnCancel = new JButton(getString("form.loading.cancel"));
        btnCancel.setEnabled(false);
        buttonPanel.add(btnCancel);

        btnInterrupt = new JButton(getString("form.loading.interrupt"));
        btnInterrupt.setEnabled(false);
        buttonPanel.add(btnInterrupt);

        buttonPanel.setMaximumSize(new Dimension((int) buttonPanel.getPreferredSize().getWidth(), (int) (btnExit.getPreferredSize().getHeight() * 4)));
        contentPane.add(buttonPanel);

        pack();

        initUIHandlers(this);

        setAlwaysOnTop(false);

        setModal(modal);

        int screenWidth = Main.frame.getRootPane().getWidth();
        int screenHeight = Main.frame.getRootPane().getHeight();
        if (devMode) {
            setMinimumSize(new Dimension((int) (screenWidth * 0.50), (int) (screenHeight * 0.50)));
        } else {
            setMinimumSize(new Dimension((int) (screenWidth * 0.30), (int) getMinimumSize().getHeight()));
            setMaximumSize(new Dimension((int) (screenWidth * 0.50), (int) getMaximumSize().getHeight()));
            setResizable(false);
        }

        setLocationRelativeTo(parent);

        setAutoRequestFocus(false);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                if(longActionTimer != null)
                    longActionTimer.stop();
            }
        });
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

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.cancel.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                Main.interrupt(processId, true);
            }
        });

        btnInterrupt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.interrupt.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                Main.interrupt(processId, false);
            }
        });

        longActionTimer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnExit.setEnabled(true);
                btnReconnect.setEnabled(true);
                btnInterrupt.setEnabled(true);
                longAction = true;
            }
        });
        longActionTimer.start();
    }

    private JPanel createTopProgressBarPanel(boolean showProgress) {
        JPanel topProgressBarPanel = new JPanel(new BorderLayout());

        JPanel messagePanel = new JPanel();
        JLabel messageLabel = new JLabel(getString("form.loading"));
        messageLabel.setFont(messageLabel.getFont().deriveFont((float) (messageLabel.getFont().getSize() * 1.5)));
        messagePanel.add(messageLabel);
        topProgressBarPanel.add(messagePanel, BorderLayout.NORTH);

        if(showProgress) {
            JPanel progressPanel = new JPanel();
            JProgressBar topProgressBar = new JProgressBar();
            topProgressBar.setIndeterminate(true);

            progressPanel.add(topProgressBar);
            topProgressBarPanel.add(progressPanel, BorderLayout.SOUTH);
        }
        return topProgressBarPanel;
    }

    public void updateBusyDialog(List<Object> input) {
        Object[] lines = input.toArray();

        if(devMode)
            setDevModeStackMessage(lines);
        else
            setStackMessage(lines);

        scrollPane.validate();
        scrollPane.repaint();
    }

    public void setDevModeStackMessage(Object[] lines) {

        if (prevLines == null)
            prevLines = new Object[lines.length];

        boolean changed = false;

        LinkedHashMap<String, Boolean> stackLines = new LinkedHashMap<>();

        boolean showTopProgressBar = true;
        boolean visibleCancelBtn = false;
        int progressBarCount = 0;
        List<Component> panels = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            Object prevLine = prevLines.length > i ? prevLines[i] : null;
            Object line = lines[i];
            if (prevLine == null || !prevLine.equals(line))
                changed = true;
            if (line instanceof ProgressBar) {
                if(progressBarCount == 0 && stackLines.isEmpty())
                    showTopProgressBar = false;
                if (!stackLines.isEmpty()) {
                    if (!panels.isEmpty())
                        panels.add(Box.createVerticalStrut(5));
                    panels.add(createTextPanel(stackLines));
                    panels.add(Box.createVerticalStrut(5));
                }
                panels.add(createProgressBarPanel((ProgressBar) line));
                stackLines = new LinkedHashMap<>();
                progressBarCount++;
            } else if(line instanceof Integer) {
                processId = (Integer) line;
            } else if (line instanceof Boolean) {
                visibleCancelBtn = true;
            } else
                stackLines.put((String) line, changed);
        }
        if(!stackLines.isEmpty()) {
            if(!panels.isEmpty())
                panels.add(Box.createVerticalStrut(5));
            panels.add(createTextPanel(stackLines));
        }

        if(longAction)
            btnCancel.setEnabled(visibleCancelBtn);

        if(showTopProgressBar) {
            if(indeterminateProgressBar) {
                while (subStackPanel.getComponentCount() > 1)
                    subStackPanel.remove(1); //оставляем верхний progressBar
            } else {
                subStackPanel.removeAll();
                addGridBagComponent(createTopProgressBarPanel(true), subStackPanel);
                indeterminateProgressBar = true; //добавляем верхний progressBar
            }
        } else {
            subStackPanel.removeAll();
            indeterminateProgressBar = false; //удаляем всё

            addGridBagComponent(createTopProgressBarPanel(false), subStackPanel);
        }

        for (Component panel : panels)
            addGridBagComponent(panel, subStackPanel);
        subStackPanel.setMaximumSize(new Dimension((int) subStackPanel.getMaximumSize().getWidth(), (int) subStackPanel.getPreferredSize().getHeight()));
        stackPanel.removeAll();
        addGridBagComponent(subStackPanel, stackPanel);

        prevLines = lines;
    }

    public void setStackMessage(Object[] lines) {

        List<JPanel> panels = new ArrayList<>();
        boolean visibleCancelBtn = false;
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
                messageProgressPanel.setBorder(new EmptyBorder(panels.isEmpty() ? 0 : 5, 5, 0, 5));
                panels.add(messageProgressPanel);
            } else if(line instanceof Integer) {
                processId = (Integer) line;
            } else if(line instanceof Boolean) {
                visibleCancelBtn = true;
            }

            btnCancel.setEnabled(visibleCancelBtn);

        }

        if(panels.isEmpty()) {
            if(indeterminateProgressBar) {
                while (subStackPanel.getComponentCount() > 1)
                    subStackPanel.remove(1); //оставляем верхний progressBar
            } else {
                subStackPanel.removeAll();
                addGridBagComponent(createTopProgressBarPanel(true), subStackPanel);
                indeterminateProgressBar = true; //добавляем верхний progressBar
            }
        } else {
            subStackPanel.removeAll();
            indeterminateProgressBar = false; //удаляем всё

            addGridBagComponent(createTopProgressBarPanel(false), subStackPanel);
            for (Component panel : panels)
                addGridBagComponent(panel, subStackPanel);
            stackPanel.removeAll();
            addGridBagComponent(subStackPanel, stackPanel);
        }
        pack();
    }

    private JTextPane createTextPanel(LinkedHashMap<String, Boolean> texts) {
        JTextPane textPane = new JTextPane();
        textPane.setBackground(null);
        textPane.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); //hack to not to scroll to the bottom (because of running with invokeLater)
        try {
            int count = 0;
            for(Map.Entry<String, Boolean> entry : texts.entrySet()) {
                count++;
                boolean changed = entry.getValue();
                textPane.getDocument().insertString(textPane.getDocument().getLength(), splitToLines(entry.getKey()) + (count == texts.size() ? "" : "\n"), changed ? highLightStyle : defaultStyle);

            }
        } catch (BadLocationException ignored) {
        }
        textPane.setMaximumSize(textPane.getPreferredSize()); //to prevent stretch
        textPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return textPane;
    }

    private JPanel createProgressBarPanel(ProgressBar progressBarLine) {
        JPanel messageProgressPanel = new JPanel(new GridLayout(progressBarLine.params == null ? 1 : 2, 1));

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
        JLabel messageLabel = new JLabel(progressBarLine.message + ": ");
        messagePanel.add(messageLabel);
        JProgressBar progressBar = new JProgressBar(0, progressBarLine.total);
        progressBar.setValue(progressBarLine.progress);
        messagePanel.add(progressBar);
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageProgressPanel.add(messagePanel);

        if (progressBarLine.params != null) {
            JLabel paramsLabel = new JLabel(progressBarLine.params);
            paramsLabel.setPreferredSize(new Dimension((int) messagePanel.getPreferredSize().getWidth(), (int) paramsLabel.getPreferredSize().getHeight()));
            messageProgressPanel.add(paramsLabel);
        }
        messageProgressPanel.setBorder(new EmptyBorder(0, 2, 0, 0));
        return messageProgressPanel;
    }

    private String splitToLines(String text) {
        FontMetrics fm = stackPanel.getFontMetrics(stackPanel.getFont());
        int maxWidth = scrollPane.getWidth() - 40;
        String[] words = text.split(" ");
        String result = "";
        String line = "";
        for(String word : words) {
            String w = word;//
            if(fm.stringWidth(line + " " + w) > maxWidth) {
                result += line + "\n";
                while (fm.stringWidth(w) > maxWidth) {
                    String leftPart = w;
                    String rightPart = "";
                    while (fm.stringWidth(leftPart) > maxWidth) {
                        rightPart = leftPart.substring(leftPart.length() - 1) + rightPart;
                        leftPart = leftPart.substring(0, leftPart.length() - 1);
                    }
                    result += leftPart + "\n";
                    w = rightPart;
                }
                line = w;
            }
            else
                line += (line.isEmpty() ? "" : " ") + w;
        }
        if(!line.isEmpty())
            result += line + "\n";
        if(result.endsWith("\n"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    private void copyToClipboard() {
        String stackMessageText = "";
        for(Component component : subStackPanel.getComponents()) {
            if(component instanceof JTextPane)
                stackMessageText += ((JTextPane) component).getText() + "\n";
        }
        if(!stackMessageText.isEmpty())
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(stackMessageText), null);
    }

    private GridBagConstraints initGridBagConstraints() {
        // weightx is 1.0 for fields, 0.0 for labels
        // gridwidth is REMAINDER for fields, 1 for labels
        GridBagConstraints fieldConstraints = new GridBagConstraints();

        // Stretch components horizontally (but not vertically)
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;

        // Components that are too short or narrow for their space
        // Should be pinned to the northwest (upper left) corner
        fieldConstraints.anchor = GridBagConstraints.NORTHWEST;

        // Give the "last" component as much space as possible
        fieldConstraints.weightx = 1.0;

        //to prevent stretch
        fieldConstraints.weighty = 1.0;

        // Give the "last" component the remainder of the row
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;

        // Add a little padding
        fieldConstraints.insets = new Insets(1, 1, 1, 1);

        return fieldConstraints;
    }

    private void addGridBagComponent(Component component, Container parent) {
        GridBagLayout gbl = (GridBagLayout) parent.getLayout();
        if(fieldConstraints != null)
            gbl.setConstraints(component, fieldConstraints);
        parent.add(component);
    }
}