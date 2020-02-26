package lsfusion.client.base.busy;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.MainController;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.ProgressBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

class BusyDialog extends JDialog {
    private static final Color NEW_STACK_MESSAGE_BACKGROUND = new Color(232, 232, 249);
    private static final Color STACK_MESSAGE_BORDER_COLOR = new Color(198, 198, 198); 

    private TopProgressBarPanel topProgressBarPanel;
    private Timer longActionTimer;
    private JButton btnCopy;
    private JButton btnExit;
    private JButton btnReconnect;
    private JButton btnCancel;
    private JButton btnInterrupt;
    private JButton btnHide;
    private JPanel subStackPanel;
    private Object[] prevLines;

    private boolean longAction;
    private Boolean needInterrupt;

    public BusyDialog(Window parent, boolean modal) {
        super(parent, getString("form.wait"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel stackPanel = new JPanel();
        stackPanel.setLayout(new BorderLayout());
        stackPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        subStackPanel = new JPanel();
        subStackPanel.setLayout(new BoxLayout(subStackPanel, BoxLayout.Y_AXIS));

        topProgressBarPanel = new TopProgressBarPanel(true);
        stackPanel.add(topProgressBarPanel, BorderLayout.NORTH);

        JPanel subStackWrapper = new JPanel(new BorderLayout());
        subStackWrapper.add(subStackPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(subStackWrapper);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        stackPanel.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(stackPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        if (MainController.configurationAccessAllowed) {
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

        btnHide = new JButton(getString("form.loading.hide"));
        buttonPanel.add(btnHide);

        buttonPanel.setMaximumSize(new Dimension(buttonPanel.getPreferredSize().width, btnExit.getPreferredSize().height * 4));
        contentPane.add(buttonPanel);

        pack();

        initUIHandlers(this);

        setAlwaysOnTop(false);

        setModal(modal);

        int screenWidth = MainFrame.instance.getRootPane().getWidth();
        int screenHeight = MainFrame.instance.getRootPane().getHeight();
        if (MainController.configurationAccessAllowed) {
            setMinimumSize(new Dimension((int) (screenWidth * 0.50), (int) (screenHeight * 0.50)));
        } else {
            setMinimumSize(new Dimension((int) (screenWidth * 0.30), getMinimumSize().height));
            setMaximumSize(new Dimension((int) (screenWidth * 0.50), getMaximumSize().height));
            setResizable(false);
        }

        setLocationRelativeTo(parent);

        setAutoRequestFocus(false);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                if (longActionTimer != null)
                    longActionTimer.stop();
            }
        });
    }

    private void initUIHandlers(final BusyDialog dialog) {
        if (MainController.configurationAccessAllowed) {
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
                MainController.shutdown();
            }
        });
        btnReconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                MainController.reconnect();
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.cancel.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                needInterrupt = false;
            }
        });

        btnInterrupt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = SwingUtils.showConfirmDialog(dialog, getString("form.loading.interrupt.confirm"), "", JOptionPane.QUESTION_MESSAGE, 1, false, 0);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
                needInterrupt = true;
            }
        });

        btnHide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainFrame.instance.setState(Frame.ICONIFIED);
            }
        });

        longActionTimer = new Timer(MainController.configurationAccessAllowed ? 5000 : 60000, new ActionListener() {
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

    public void updateBusyDialog(List<Object> input) {
        Object[] lines = input.toArray();

        if (MainController.configurationAccessAllowed)
            setStackMessageDevMode(lines);
        else
            setStackMessage(lines);

        validate();
        repaint();
    }

    public Boolean needInterrupt() {
        Boolean value = needInterrupt;
        needInterrupt = null;
        return value;
    }

    public void setStackMessageDevMode(Object[] lines) {
        if (prevLines == null)
            prevLines = new Object[lines.length];

        boolean changed = false;
        boolean showTopProgressBar = true;
        boolean enableCancelBtn = false;
        int progressBarCount = 0;
        List<JComponent> stackComponents = new ArrayList<>();
        List<String> stackLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            Object line = lines[i];
            if (line instanceof ProgressBar) {
                if (progressBarCount == 0 && stackLines.isEmpty())
                    showTopProgressBar = false;
                if (!stackLines.isEmpty()) {
                    stackComponents.add(createTextPanel(stackLines, changed));
                    stackLines = new ArrayList<>();
                    changed = false;
                }
                JPanel progressBarPanel = createProgressBarPanel((ProgressBar) line, ((ProgressBar) line).getParams() == null ? 1 : 2);
                progressBarPanel.setBorder(new EmptyBorder(0, 2, 0, 0));
                stackComponents.add(progressBarPanel);
                progressBarCount++;
            } else if (line instanceof Boolean) {
                enableCancelBtn = true;
            } else {
                assert line instanceof String;
                Object prevLine = prevLines.length > i ? prevLines[i] : null;
                changed = prevLine == null || !prevLine.equals(line);
                stackLines.add((String) line);
            }
        }
        if (!stackLines.isEmpty()) {
            stackComponents.add(createTextPanel(stackLines, changed));
        }

        refreshSubStackPanel(stackComponents);

        if (longAction)
            btnCancel.setEnabled(enableCancelBtn);

        topProgressBarPanel.showProgressBar(showTopProgressBar);

        prevLines = lines;
    }
    
    private void refreshSubStackPanel(List<JComponent> stackComponents) {
        subStackPanel.removeAll();
        for (JComponent stackComponent : stackComponents) {
            stackComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            subStackPanel.add(stackComponent);
            subStackPanel.add(Box.createVerticalStrut(5));
        }            
    }

    public void setStackMessage(Object[] lines) {
        boolean enableCancelBtn = false;

        List<JComponent> stackComponents = new ArrayList<>();
        for (Object line : lines) {
            if (line instanceof ProgressBar) {
                JPanel progressBarPanel = createProgressBarPanel((ProgressBar) line, 2);
                progressBarPanel.setBorder(new EmptyBorder(subStackPanel.getComponentCount() > 0 ? 0 : 5, 5, 0, 5));
                stackComponents.add(progressBarPanel);
            } else if (line instanceof Boolean) {
                enableCancelBtn = true;
            }
        }

        refreshSubStackPanel(stackComponents);
        
        btnCancel.setEnabled(enableCancelBtn);

        topProgressBarPanel.showProgressBar(subStackPanel.getComponentCount() == 0);

        pack();
    }

    private JTextArea createTextPanel(List<String> lines, boolean changed) {
        final JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        Font font = UIManager.getFont("Label.font");
        textArea.setFont(font);
        final int fontHeight = textArea.getFontMetrics(font).getHeight();
        
        textArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); //hack to not to scroll to the bottom (because of running with invokeLater)
        
        String str = "";
        for (int i = 0; i < lines.size(); i++) {
            str += lines.get(i) + (i == lines.size() - 1 ? "" : "\n");
        }
        textArea.setText(str);
        textArea.setBackground(changed ? NEW_STACK_MESSAGE_BACKGROUND : null);
        
        textArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                textArea.setPreferredSize(new Dimension(subStackPanel.getPreferredSize().width - 40, getLineCountAsSeen(textArea, fontHeight) * fontHeight + 10));
            }
        });

        textArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(STACK_MESSAGE_BORDER_COLOR, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        return textArea;
    }

    private int getLineCountAsSeen(JTextComponent txtComp, int fontHeight) { // количество строк с учётом переносов. работает после отрисовки
        try {
            int height = txtComp.modelToView(txtComp.getDocument().getEndPosition().getOffset() - 1).y;
            return height / fontHeight + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private JPanel createProgressBarPanel(ProgressBar progressBarLine, int rows) {
        JPanel messageProgressPanel = new JPanel(new GridLayout(rows, 1));

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
        JLabel messageLabel = new JLabel(progressBarLine.message + ": ");
        messagePanel.add(messageLabel);
        JProgressBar progressBar = new JProgressBar(0, progressBarLine.total);
        progressBar.setValue(progressBarLine.progress);
        messagePanel.add(progressBar);
        messageProgressPanel.add(messagePanel);

        String params = progressBarLine.getParams();
        if (params != null) {
            JLabel paramsLabel = new JLabel(params);
            paramsLabel.setPreferredSize(new Dimension(messagePanel.getPreferredSize().width, paramsLabel.getPreferredSize().height));
            messageProgressPanel.add(paramsLabel);
        }
        return messageProgressPanel;
    }

    private void copyToClipboard() {
        String stackMessageText = "";
        for (Component component : subStackPanel.getComponents()) {
            if (component instanceof JTextArea)
                stackMessageText += ((JTextArea) component).getText() + "\n";
        }
        if (!stackMessageText.isEmpty())
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(stackMessageText), null);
    }

    private class TopProgressBarPanel extends JPanel {
        JPanel progressPanel;

        public TopProgressBarPanel(boolean showProgress) {
            super(new BorderLayout());

            JPanel messagePanel = new JPanel();
            JLabel messageLabel = new JLabel(getString("form.loading"));
            messageLabel.setFont(messageLabel.getFont().deriveFont((float) (messageLabel.getFont().getSize() * 1.5)));
            messagePanel.add(messageLabel);
            add(messagePanel, BorderLayout.NORTH);

            progressPanel = new JPanel();
            final JProgressBar topProgressBar = new JProgressBar();
            topProgressBar.setIndeterminate(true);
            topProgressBar.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    topProgressBar.setIndeterminate(!topProgressBar.isIndeterminate());
                }
            });

            progressPanel.add(topProgressBar);
            add(progressPanel, BorderLayout.SOUTH);

            showProgressBar(showProgress);
        }

        void showProgressBar(boolean showProgress) {
            progressPanel.setVisible(showProgress);
        }
    }
}