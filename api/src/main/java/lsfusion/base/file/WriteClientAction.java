package lsfusion.base.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import org.apache.commons.io.FilenameUtils;
import org.jfree.ui.ExtensionFileFilter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static lsfusion.base.ApiResourceBundle.getString;

public class WriteClientAction extends ExecuteClientAction {
    public final NamedFileData file;
    public final String path;
    public final boolean append;
    public final boolean isDialog;

    public WriteClientAction(File file, String path) throws IOException {
        this(new NamedFileData(new RawFileData(file)), path, false, true);
    }

    @Deprecated
    public WriteClientAction(RawFileData file, String path, String extension, boolean append, boolean isDialog) {
        this(new NamedFileData(new FileData(file, extension)), path, append, isDialog);
    }

    public WriteClientAction(NamedFileData file, String path, boolean append, boolean isDialog) {
        this.file = file;
        this.path = path;
        this.append = append;
        this.isDialog = isDialog;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        try {
            String filePath = path;
            if (isDialog)
                filePath = FilenameUtils.removeExtension(showSaveFileDialog(new File(WriteUtils.appendExtension(path, file))));
            if (filePath != null) {
                WriteUtils.write(file, filePath, true, append);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String showSaveFileDialog(File file) {
        String chosenFile = null;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(file);

        String extension = BaseUtils.getFileExtension(file);
        if (!BaseUtils.isRedundantString(extension)) {
            ExtensionFileFilter filter = new ExtensionFileFilter("." + extension, extension);
            fileChooser.addChoosableFileFilter(filter);
        }
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            chosenFile = fileChooser.getSelectedFile().getAbsolutePath();
            SystemUtils.saveCurrentDirectory(new File(chosenFile).getParentFile());
        }

        if (chosenFile != null && new File(chosenFile).exists()) {
            int answer = showConfirmDialog(fileChooser, getString("layout.menu.file.already.exists.replace"),
                    getString("layout.menu.file.already.exists"));
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return chosenFile;
    }

    private int showConfirmDialog(Component parentComponent, Object message, String title) {
        Object[] options = {UIManager.getString("OptionPane.yesButtonText"),
                UIManager.getString("OptionPane.noButtonText")};

        JOptionPane dialogPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[0]);

        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("RIGHT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("UP"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("LEFT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("DOWN"));

        dialogPane.createDialog(parentComponent, title).setVisible(true);
        if (dialogPane.getValue() == JOptionPane.UNINITIALIZED_VALUE)
            return 0;
        if (dialogPane.getValue() == options[0]) {
            return JOptionPane.YES_OPTION;
        } else {
            return JOptionPane.NO_OPTION;
        }
    }

    private void addFocusTraversalKey(Component comp, int id, KeyStroke key) {
        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }
}