package lsfusion.base.file;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import org.jfree.ui.ExtensionFileFilter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;

import static lsfusion.base.ApiResourceBundle.getString;

public class FileDialogUtils {

    public static String showSaveFileDialog(String path, RawFileData file) {
        Map<String, RawFileData> chosenFiles = showSaveFileDialog(Collections.singletonMap(path, file));
        return chosenFiles.isEmpty() ? null : BaseUtils.singleKey(chosenFiles);
    }

    @Deprecated
    public static Map<String, RawFileData> showSaveFileDialog(Map<String, RawFileData> files) {
        Map<String, RawFileData> resultMap = new HashMap<>();
        JFileChooser fileChooser = new JFileChooser();
        Map<String, String> chosenFiles = chooseFiles(fileChooser, files.keySet());

        for (Map.Entry<String, String> chosenFile : chosenFiles.entrySet()) {
            File file = new File(chosenFile.getValue());
            if (chosenFiles.size() == 1 && file.exists()) {
                int answer = showConfirmDialog(fileChooser, getString("layout.menu.file.already.exists.replace"),
                        getString("layout.menu.file.already.exists"), JOptionPane.QUESTION_MESSAGE, false);
                if (answer != JOptionPane.YES_OPTION) {
                    break;
                }
            }
            resultMap.put(chosenFile.getValue(), files.get(chosenFile.getKey()));
        }
        return resultMap;
    }

    public static Map<String, String> chooseFiles(JFileChooser fileChooser, Set<String> files) {
        Map<String, String> result = new HashMap<>();
        fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
        boolean singleFile;
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (files.size() > 1) {
            singleFile = false;
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            singleFile = true;
            File file = new File(files.iterator().next());
            fileChooser.setSelectedFile(file);
            String extension = BaseUtils.getFileExtension(file);
            if (!BaseUtils.isRedundantString(extension)) {
                ExtensionFileFilter filter = new ExtensionFileFilter("." + extension, extension);
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            for (String file : files) {
                if (singleFile) {
                    result.put(file, path);
                } else {
                    result.put(file, path + "\\" + file);
                }
            }
            SystemUtils.saveCurrentDirectory(singleFile ? new File(path).getParentFile() : new File(path));
        }
        return result;
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, boolean cancel) {
        return showConfirmDialog(parentComponent, message, title, messageType, 0, cancel, 0);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, int initialValue,
                                        boolean cancel, int timeout) {

        Object[] options = {UIManager.getString("OptionPane.yesButtonText"),
                UIManager.getString("OptionPane.noButtonText")};
        if (cancel) {
            options = BaseUtils.add(options, UIManager.getString("OptionPane.cancelButtonText"));
        }

        JOptionPane dialogPane = new JOptionPane(message,
                messageType,
                cancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION,
                null, options, options[initialValue]);

        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("RIGHT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("UP"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("LEFT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("DOWN"));

        final JDialog dialog = dialogPane.createDialog(parentComponent, title);
        if (timeout != 0) {
            final java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer.cancel();
                    dialog.setVisible(false);
                }
            }, timeout);
        }
        dialog.setVisible(true);

        if (dialogPane.getValue() == JOptionPane.UNINITIALIZED_VALUE)
            return initialValue;
        if (dialogPane.getValue() == options[0]) {
            return JOptionPane.YES_OPTION;
        } else {
            if (!cancel || dialogPane.getValue() == options[1])
                return JOptionPane.NO_OPTION;
            else
                return JOptionPane.CANCEL_OPTION;
        }
    }

    public static void addFocusTraversalKey(Component comp, int id, KeyStroke key) {
        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }
}