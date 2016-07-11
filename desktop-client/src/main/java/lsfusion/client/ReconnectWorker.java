package lsfusion.client;

import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.remote.RMIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static lsfusion.client.ClientResourceBundle.getString;

public final class ReconnectWorker extends SwingWorker<RemoteLogicsLoaderInterface, Integer> {
    private final ProgressDialog dlg;

    private final String serverHost;
    private final String serverDB;
    private final int serverPort;

    private RemoteLogicsLoaderInterface remoteLoader;

    public ReconnectWorker(String serverHost, String serverPort, String serverDB) {
        this.serverHost = serverHost;
        this.serverPort = Integer.parseInt(serverPort);
        this.serverDB = serverDB;

        Main.overrideRMIHostName(serverHost);

        dlg = new ProgressDialog();
    }

    @Override
    protected RemoteLogicsLoaderInterface doInBackground() throws Exception {
        remoteLoader = null;
        int attempts = 0;
        while (true) {
            publish(attempts++);
            try {
                return (RemoteLogicsLoaderInterface)RMIUtils.rmiLookup(serverHost, serverPort, serverDB, "RemoteLogicsLoader", Main.rmiSocketFactory);
            } catch (ConnectException | NotBoundException | NoSuchObjectException ignore) {
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

    public RemoteLogicsLoaderInterface connect() throws Throwable {
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
