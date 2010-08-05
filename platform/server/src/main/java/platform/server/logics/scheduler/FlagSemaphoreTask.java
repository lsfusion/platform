package platform.server.logics.scheduler;

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
                System.out.println("Ошибка при импорте файла : " + e.getMessage());
            } finally {

                if (flagLock != null) {
                    try {
                        flagLock.release();
                    } catch (IOException e) {
                        System.out.println("Удаление лока на флаг : " + e.getMessage());
                    }
                }

                if (flagAccess != null) {
                    try {
                        flagAccess.close();
                    } catch (IOException e) {
                        System.out.println("Закрытие канала чтения флага: " + e.getMessage());
                    }
                }
            }

            if (succeed) {
                if (!flagFile.delete()) {
                    System.out.println("Не удалось удалить файл флага");
                }
            }
        }

    }
}
