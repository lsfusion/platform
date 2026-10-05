package lsfusion.server.base.controller.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.WriteServerFileClientAction;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// the files the clients read in chunks (WriteServerFileClientAction -> RemoteRequestInterface.readFileChunk) :
// a file is read only by the connection that registered it, by a random id, and is dropped after the last chunk is read
// or after it was not read for a while (the user cancelled the save dialog, the client is gone)
public class RemoteFiles {

    private static final long EXPIRE_MILLIS = TimeUnit.HOURS.toMillis(1);

    private static class RemoteFile {
        private final File file;
        private final boolean temporary; // delete the file when it is dropped
        private final Long connection;
        private volatile long lastAccess = System.currentTimeMillis();

        private RemoteFile(File file, boolean temporary, Long connection) {
            this.file = file;
            this.temporary = temporary;
            this.connection = connection;
        }
    }

    private static final Map<String, RemoteFile> files = new ConcurrentHashMap<>();

    // dropping by a timer and not on the next transfer, since there may be no next one, and a temporary file can be gigabytes
    private static final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RemoteFiles cleaner");
        thread.setDaemon(true);
        return thread;
    });
    static {
        cleaner.scheduleWithFixedDelay(RemoteFiles::dropExpired, 10, 10, TimeUnit.MINUTES);
    }

    public static WriteServerFileClientAction getWriteAction(File file, boolean temporary, String path, String extension, boolean isDialog) {
        return new WriteServerFileClientAction(register(file, temporary), path, extension, isDialog);
    }

    public static String register(File file, boolean temporary) {
        String fileId = UUID.randomUUID().toString();
        files.put(fileId, new RemoteFile(file, temporary, ThreadLocalContext.getCurrentConnection()));
        return fileId;
    }

    public static byte[] read(String fileId, long offset, int length) throws IOException {
        RemoteFile remoteFile = files.get(fileId);
        if (remoteFile == null || !Objects.equals(remoteFile.connection, ThreadLocalContext.getCurrentConnection()))
            throw new IllegalStateException("File " + fileId + " is not found (it has been already read or it has expired)");
        remoteFile.lastAccess = System.currentTimeMillis();

        int readLength = Math.min(length, WriteServerFileClientAction.CHUNK_SIZE);
        byte[] chunk;
        try (RandomAccessFile file = new RandomAccessFile(remoteFile.file, "r")) {
            chunk = new byte[(int) Math.max(Math.min(readLength, file.length() - offset), 0)];
            file.seek(offset);
            file.readFully(chunk);
        }
        if (chunk.length < readLength) // the last chunk
            drop(fileId);
        return chunk;
    }

    private static void drop(String fileId) {
        RemoteFile remoteFile = files.remove(fileId);
        if (remoteFile != null && remoteFile.temporary)
            BaseUtils.safeDelete(remoteFile.file);
    }

    private static void dropExpired() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, RemoteFile> entry : files.entrySet())
                if (now - entry.getValue().lastAccess > EXPIRE_MILLIS)
                    drop(entry.getKey());
        } catch (Throwable t) { // an exception would cancel the next runs
            ServerLoggers.systemLogger.error("RemoteFiles cleanup failed", t);
        }
    }
}
