package lsfusion.client.base.log;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.remote.ConnectionLostManager;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.base.exception.RemoteInternalException;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.view.SwingDefaults.getRequiredForeground;

public final class Log {
    public static Logger logger = ClientLoggers.clientLogger;

    public static void log(String message) {
        logger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    private static String text = "";

    private static WeakReference<LogPanel> logPanelRef = new WeakReference<>(null);

    public static JPanel recreateLogPanel() {
        LogPanel logPanel = new LogPanel();

        logPanelRef = new WeakReference<>(logPanel);
        text = "";

        return logPanel;
    }

    private static LogPanel getLogPanel() {
        LogPanel logPanel = logPanelRef.get();
        // пока таким образом определим есть ли он на экране
        if (logPanel != null && logPanel.getTopLevelAncestor() != null) {
            return logPanel;
        }

        return null;
    }

    private static void print(String itext) {
        text += itext;
        stateChanged();
    }

    private static void println(String itext) {
        print(itext + '\n');
    }

    private static void printmsg(String itext) {
        println(getMsgHeader() + itext + getMsgFooter());
    }

    private static String getMsgHeader() {
        return "--- " + DateFormat.getInstance().format(new Date(System.currentTimeMillis())) + " ---\n";
    }

    private static String getMsgFooter() {
        return "";
    }

    private static void stateChanged() {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.updateText(text);
        }
    }

    private static void provideSuccessFeedback() {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.setTemporaryBackground(SwingDefaults.getSelectionColor());
        }
    }

    private static void provideErrorFeedback() {
        LogPanel logPanel = getLogPanel();
        if (logPanel != null) {
            logPanel.setTemporaryBackground(SwingDefaults.getLogPanelErrorColor());
            logPanel.provideErrorFeedback();
        }
    }

    public static void message(String message) {
        message(message, true);
    }

    public static void message(String message, boolean successFeedback) {
        printmsg(message);
        logger.info(message);
        if (successFeedback) {
            provideSuccessFeedback();
        }
    }

    public static void error(Throwable remote) {
        assert remote.getCause() == null;
    
        String message = remote.getMessage();
        if(remote instanceof RemoteInternalException)
            message += "\n" + getString("errors.contact.administrator");

        Pair<String, String> exStacks = RemoteInternalException.getExStacks(remote);
        error(message, null, null, exStacks.first, exStacks.second, false);
    }

    public static void messageWarning(String message, List<String> titles, List<List<String>> data) {
        error(message, titles, data, "", null, true);
    }
    
    public static void error(String message, List<String> titles, List<List<String>> data, String javaStack, String lsfStack, boolean warning) {
        if (ConnectionLostManager.isConnectionLost()) {
            return;
        }

        SwingUtils.assertDispatchThread();

        printmsg(message);
        logger.error(message);

        provideErrorFeedback();

        if (MainFrame.instance == null) {
            return;
        }

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));

        JTextPane titlePanel = new JTextPane();
        titlePanel.setContentType("text/html");
        titlePanel.setText(toHtml(message));
        titlePanel.setEditable(false);
        titlePanel.setBackground(null);
        titlePanel.setBorder(null);

        double screenWidth = (MainFrame.instance != null ? MainFrame.instance.getRootPane().getWidth() : Toolkit.getDefaultToolkit().getScreenSize().width)  * 0.9;
        double titleWidth = titlePanel.getPreferredSize().getWidth();
        double titleHeight = titlePanel.getPreferredSize().getHeight();
        titlePanel.setPreferredSize(new Dimension((int) Math.min(screenWidth, titleWidth), (int) (titleHeight * Math.ceil(titleWidth / screenWidth))));
        labelPanel.add(titlePanel);
        labelPanel.add(Box.createHorizontalGlue());
        
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.add(labelPanel);
        if (data != null && !titles.isEmpty()) {
            messagePanel.add(new JScrollPane(createDataTable(titles, data)));
        }

        Font logFont = new Font("Tahoma", Font.PLAIN, MainFrame.getIntUISize(12));
        JTextArea javaStackTA = new JTextArea(javaStack, 7, 60);
        javaStackTA.setFont(logFont);
        javaStackTA.setForeground(getRequiredForeground());
        JScrollPane javaStackInScroll = new JScrollPane(javaStackTA);

        JPanel textWithLine = new JPanel();
        textWithLine.setLayout(new BorderLayout(10, 10));
        textWithLine.add(new JSeparator(), BorderLayout.NORTH);
        if (lsfStack != null) {
            JTabbedPane stackPanes = new JTabbedPane();
            stackPanes.add("Java", javaStackInScroll);
            
            JTextArea lsfStackTA = new JTextArea(lsfStack, 7, 60);
            lsfStackTA.setFont(logFont);
            lsfStackTA.setForeground(getRequiredForeground());
            stackPanes.add("LSF", new JScrollPane(lsfStackTA));
            
            textWithLine.add(stackPanes);
        } else {
            textWithLine.add(javaStackInScroll);
        }

        final JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setVisible(false);
        south.add(Box.createVerticalStrut(10));

        south.add(textWithLine);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(messagePanel);
        mainPanel.add(south);

        String opt[];
        final String okOption = getString("dialog.ok");
        final String moreOption = getString("client.more");
        if (javaStack.length() > 0) {
            opt = new String[]{okOption, moreOption};
        } else {
            opt = new String[]{okOption};
        }
        final JOptionPane optionPane = new JOptionPane(mainPanel, warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE,
                                     JOptionPane.YES_NO_OPTION,
                                     null,
                                     opt,
                okOption);

        final JDialog dialog = new JDialog(MainFrame.instance, MainFrame.instance  != null ? MainFrame.instance.getTitle() : "lsfusion", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(optionPane);
        dialog.setMinimumSize(dialog.getPreferredSize());
        dialog.pack();

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                Object value = optionPane.getValue();
                if (dialog.isVisible() && (value.equals(okOption) || value.equals(-1))) {
                    dialog.dispose();
                } else if (value.equals(moreOption)) {
                    boolean southWasVisible = south.isVisible();
                    south.setVisible(!southWasVisible);
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    dialog.setMinimumSize(dialog.getPreferredSize());
                    if (southWasVisible) {
                        dialog.pack();
                    }
                }
            }
        });


        //центрируем на экране
        dialog.setLocationRelativeTo(dialog.getOwner());

        dialog.setVisible(true);
    }

    private static Component createDataTable(List<String> titles, List<List<String>> data) {
        int size = data.size();
        Object columnNames[] = titles.toArray();
        Object dataArray[][] = new Object[size][];
        int i = 0;
        for (List<String> dataRow : data) {
            dataArray[i] = dataRow.toArray();
            i++;
        }
        JTable table = new JTable(dataArray, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        if(titles.size() == 1) {
            table.getTableHeader().setReorderingAllowed(false);
        }
        table.setPreferredScrollableViewportSize(
                new Dimension(
                        table.getPreferredScrollableViewportSize().width,
                        Math.min(table.getPreferredScrollableViewportSize().height, dataArray.length * table.getRowHeight())
                ));

        Caret caret = new DefaultCaret()
        {
            public void focusGained(FocusEvent e)
            {
                setVisible(true);
                setSelectionVisible(true);
            }
        };
        caret.setBlinkRate( UIManager.getInt("TextField.caretBlinkRate") );

        JTextField textField = new JTextField();
        textField.setEditable(false);
        textField.setCaret(caret);
        textField.setBorder(new LineBorder(Color.BLACK));

        DefaultCellEditor dce = new DefaultCellEditor(textField);
        for(int j = 0; j < table.getColumnModel().getColumnCount(); j++) {
            table.getColumnModel().getColumn(j).setCellEditor(dce); 
        }
       
        table.setFocusable(true);
        return table;
    }

    private static String toHtml(String message) {
        return "<html><font size=+1>" + StringEscapeUtils.escapeHtml4(message).replaceAll("(\r\n|\n\r|\r|\n)", "<br />") + "</font></html>";
    }
}
