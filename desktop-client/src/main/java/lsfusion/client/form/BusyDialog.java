package lsfusion.client.form;

import lsfusion.client.Main;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
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
    private JProgressBar progressBar;
    private Style defaultStyle;
    private Style highLightStyle;
    private JTextPane stackMessage;
    private String[] prevLines;
    static boolean devMode = Main.configurationAccessAllowed;

    public BusyDialog(Window parent, boolean modal) {
        super(parent, getString("form.wait"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        statusMessage = new JLabel(getString("form.loading"));
        JPanel messagePanel = new JPanel();
        messagePanel.add(statusMessage);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel progressPanel = new JPanel();
        progressPanel.add(progressBar);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(messagePanel, BorderLayout.NORTH);
        topPanel.add(progressPanel, BorderLayout.SOUTH);
        topPanel.setMaximumSize(new Dimension((int) topPanel.getPreferredSize().getWidth(), (int) (progressBar.getPreferredSize().getHeight() * 4)));

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
            stackMessage = new JTextPane();
            stackMessage.setMargin(new Insets(10, 10, 10, 10));
            stackMessage.setEditable(false);
            stackMessage.setBackground(null);
            JScrollPane stackPanel = new JScrollPane(stackMessage);
            stackPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            contentPane.add(stackPanel);

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

        if (devMode) {
            FontMetrics fm = stackMessage.getFontMetrics(stackMessage.getFont());
            stackMessage.setMinimumSize(new Dimension(stackMessage.getWidth(), fm.getHeight() * 20)); //20 lines
            setMinimumSize(new Dimension((int) (Main.frame.getRootPane().getWidth() * 0.50), fm.getHeight() * 27));
        } else {
            setResizable(false);
        }

        setLocationRelativeTo(parent);

        setAutoRequestFocus(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                lockFrame();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                //unlockFrame();
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

    public void setStackMessage(String input) {

        String[] lines = input.split("\\n");
        if (prevLines == null)
            prevLines = new String[lines.length];

        int offset = 0;
        stackMessage.setText("");
        boolean changed = false;

        for (int i = 0; i < lines.length; i++) {
            String prevLine = prevLines.length > i ? prevLines[i] : null;
            String line = lines[i];
            if (prevLine == null || !prevLine.equals(line))
                changed = true;
            try {
                stackMessage.getDocument().insertString(offset, line + "\n", changed ? highLightStyle : defaultStyle);
            } catch (BadLocationException ignored) {
            }
            offset += line.length() + 1;
        }
        prevLines = lines;

    }

    private void copyToClipboard() {
        String stackMessageText = stackMessage.getText();
        if(stackMessageText != null)
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(stackMessage.getText()), null);
    }

}