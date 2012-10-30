package tmc.integration.scheduler;

import platform.server.logics.ServerResourceBundle;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public abstract class FlagSemaphoreTask {

    protected abstract void run() throws Exception;

    protected byte[] flagContent;

    public static void run(String flagName, FlagSemaphoreTask task) throws Exception {

        File flagFile = new File(flagName);

        if (flagFile.exists()) {

            boolean succeed = false;

            RandomAccessFile flagAccess = null;
            FileChannel flagChannel = null;
            FileLock flagLock = null;
            try {

                flagAccess = new RandomAccessFile(flagFile, "rw");
                flagChannel = flagAccess.getChannel();
                flagLock = flagChannel.tryLock();

                if (!flagLock.isShared()) {

                    task.flagContent = new byte[(int)flagAccess.length()];
                    flagAccess.read(task.flagContent);

                    task.run();
                    succeed = true;
                }

            } catch (IOException e) {
                System.out.println(ServerResourceBundle.getString("logics.scheduler.error.importing.file")+" " + e.getMessage());
            } finally {

                if (flagLock != null) {
                    try {
                        flagLock.release();
                    } catch (IOException e) {
                        System.out.println(ServerResourceBundle.getString("logics.scheduler.deleting.flag.lock")+" " + e.getMessage());
                    }
                }

                if (flagAccess != null) {
                    try {
                        flagAccess.close();
                    } catch (IOException e) {
                        System.out.println(ServerResourceBundle.getString("logics.scheduler.closing.channel.flag.reading")+" " + e.getMessage());
                    }
                }
            }

            if (succeed) {
                if (!flagFile.delete()) {
                    System.out.println(ServerResourceBundle.getString("logics.scheduler.failed.to.delete.flag.file"));
                }
            }
        }

    }
}
