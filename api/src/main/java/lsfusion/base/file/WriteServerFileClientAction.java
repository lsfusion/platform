package lsfusion.base.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import lsfusion.interop.base.remote.RemoteRequestInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// writes a file that stays on the server : the client reads it in chunks (RemoteRequestInterface.readFileChunk),
// while WriteClientAction carries the whole file in one byte array, so a file bigger than the max java array (2GB)
// or than the memory of the app server / web server / client can be written only this way (backups, heap dumps)
public class WriteServerFileClientAction extends ExecuteClientAction {

    public static final int CHUNK_SIZE = 16 * 1024 * 1024;

    public final String fileId;
    public final String path;
    public final String extension;
    public final boolean isDialog;

    public WriteServerFileClientAction(String fileId, String path, String extension, boolean isDialog) {
        this.fileId = fileId;
        this.path = path;
        this.extension = extension;
        this.isDialog = isDialog;
    }

    public String getFileName() {
        return BaseUtils.addExtension(path, extension);
    }

    public void read(RemoteRequestInterface remote, OutputStream out) throws IOException {
        long offset = 0;
        byte[] chunk;
        do {
            chunk = remote.readFileChunk(fileId, offset, CHUNK_SIZE);
            out.write(chunk);
            offset += chunk.length;
        } while (chunk.length == CHUNK_SIZE);
    }

    // desktop client
    public void write(RemoteRequestInterface remote) throws IOException {
        String fileName = getFileName();
        File file;
        if (isDialog) {
            String chosenFile = WriteClientAction.showSaveFileDialog(new File(fileName));
            if (chosenFile == null)
                return;
            if (!BaseUtils.isRedundantString(extension) && !chosenFile.endsWith("." + extension))
                chosenFile = chosenFile + "." + extension;
            file = new File(chosenFile);
        } else
            file = new File(System.getProperty("user.home") + "/Downloads/", fileName);

        // written next to the destination and moved over it only when the whole file is read : a cut transfer
        // must not destroy the file being replaced (or leave a truncated one that looks whole)
        File partFile = new File(file.getPath() + ".part");
        try {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(partFile))) {
                read(remote, out);
            }
            Files.move(partFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (partFile.exists()) // not moved, the transfer failed
                BaseUtils.safeDelete(partFile);
        }
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "WriteServerFileClientAction[" + getFileName() + "]";
    }
}
