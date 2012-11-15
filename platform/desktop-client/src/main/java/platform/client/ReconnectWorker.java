package platform.client;

import platform.interop.RemoteLoaderInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static platform.client.ClientResourceBundle.getString;

public final class ReconnectWorker extends SwingWorker<RemoteLoaderInterface, Integer> {
    private ProgressDialog dlg;

    private String serverUrl;

    private RemoteLoaderInterface remoteLoader;

    public ReconnectWorker(String serverHost, String serverPort, String serverDB) {
        this.serverUrl = MessageFormat.format("rmi://{0}:{1}/{2}/BusinessLogicsLoader", serverHost, serverPort, serverDB);
        dlg = new ProgressDialog();
    }

    @Override
    protected RemoteLoaderInterface doInBackground() throws Exception {
        remoteLoader = null;
        int attempts = 0;
        while (true) {
            publish(attempts++);
            try {
                remoteLoader = (RemoteLoaderInterface) Naming.lookup(serverUrl);
            } catch (ConnectException ignore) {
            } catch (NoSuchObjectException ignore) {
            } catch (NotBoundException ignore) {
            }

            if (remoteLoader != null) {
                return remoteLoader;
            }

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

    public RemoteLoaderInterface connect() throws Throwable {
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
            setLocationRelativeTo(null);
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
        }

        private void setupDialogForDevMode() {
            //чтобы диалог не забирал фокус
            if (StartupProperties.preventBlockerActivation) {
                setFocusableWindowState(false);
                setAlwaysOnTop(false);
            }
        }

        private void initUIHandlers() {
            btnCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cancel(true);
                }
            });

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    cancel(true);
                }
            });
        }

        public void setAttemptNumber(int n) {
            lbMessage.setText(getString("connect.message", n));
        }
    }
}
