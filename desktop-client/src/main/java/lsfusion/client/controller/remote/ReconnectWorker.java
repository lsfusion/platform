package lsfusion.client.controller.remote;

import lsfusion.client.StartupProperties;
import lsfusion.client.controller.MainController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static lsfusion.client.ClientResourceBundle.getString;

public final class ReconnectWorker<T> extends SwingWorker<T, Integer> {
    private final ProgressDialog dlg;
    
    private final Callable<T> callable;

    public ReconnectWorker(Callable<T> callable) {
        this.callable = callable;

        dlg = new ProgressDialog();
    }

    @Override
    protected T doInBackground() throws Exception {
        int attempts = 0; 
        while (true) {
            publish(attempts++);

            T result = callable.call();
            if(result != null)
                return result;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    @Override
    protected void process(List<Integer> chunks) {
        dlg.setAttemptNumber(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        dlg.setVisible(false);
        dlg.dispose();
    }

    public T connect() throws Throwable {
        execute();
        dlg.setVisible(true);

        try {
            return get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private class ProgressDialog extends JDialog {
        private JButton btnCancel;
        private JLabel lbMessage;

        public ProgressDialog() {
            super((Frame) null, getString("connect.title"), true);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            setAlwaysOnTop(true);

            lbMessage = new JLabel();
            btnCancel = new JButton(getString("connect.cancel"));

            setAttemptNumber(1);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(btnCancel);

            JPanel messagePanel = new JPanel();
            messagePanel.add(lbMessage);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JPanel progressPanel = new JPanel();
            progressPanel.add(progressBar);

            Container contentPane = getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            contentPane.add(messagePanel);
            contentPane.add(progressPanel);
            contentPane.add(buttonPanel);

            pack();
            setResizable(false);

            initUIHandlers();

            setupDialogForDevMode();

            //не даём забрать фокус диалогу, потому что при открытии modal form в onDesktopClientStarted фокус у ReconnectWorker ещё никто не успевает отобрать
            setFocusableWindowState(false);

            setLocationRelativeTo(null);
        }

        private void setupDialogForDevMode() {
            //чтобы диалог не забирал фокус
            if (StartupProperties.preventBlockerActivation) {
                setFocusableWindowState(false);
                setAlwaysOnTop(false);
            }
        }

        private void initUIHandlers() {
            btnCancel.addActionListener(e -> {
                cancel(true);
                MainController.shutdown();
            });

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    cancel(true);
                }
            });
        }

        public void setAttemptNumber(int n) {
            lbMessage.setText(n == 0 ? getString("connect.message", n + 1) : getString("connect.message.unavailable", n + 1));
        }

        @Override
        public void dispose() {
            super.dispose();
            cancel(true);
        }
    }
}
