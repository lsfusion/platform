package lsfusion.client.form;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Main;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class BusyDialog extends JDialog {

    //private JButton btnCancel;
    private JLabel statusMessage;
    private JProgressBar progressBar;
    private Style defaultStyle;
    private Style highLightStyle;
    private JTextPane stackMessage;
    private String[] prevLines;
    static boolean devMode = Main.configurationAccessAllowed;

    public BusyDialog(Window parent, boolean modal) {
        super(parent, ClientResourceBundle.getString("form.wait"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        statusMessage = new JLabel(ClientResourceBundle.getString("form.loading"));
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
        }

        //JPanel buttonPanel = new JPanel();
        //buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        //btnCancel = new JButton("Cancel");
        //buttonPanel.add(btnCancel);
        //contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();

        //initUIHandlers();

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

        final Component focusOwner = Main.frame.getMostRecentFocusOwner();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                Main.frame.setLocked(true);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                Main.frame.setLocked(false);
                if(focusOwner != null)
                    focusOwner.requestFocusInWindow();
            }
        });
    }

//    private void initUIHandlers() {
//        btnCancel.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                cancel();
//            }
//        });
//
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosed(WindowEvent e) {
//                cancel();
//            }
//        });
//    }

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

//    private void cancel() {
//        this.dispose();
//    }

}