package platform.server.logics.scheduler;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public abstract class FlagSemaphoreTask {

    protected abstract void run() throws Exception;

    public static void run(String flagName, FlagSemaphoreTask task) throws Exception {

        File flagFile = new File(flagName);

        if (flagFile.exists()) {

            boolean succeed = false;

            FileChannel flagChannel = null;
            FileLock flagLock = null;
            try {

                flagChannel = new RandomAccessFile(flagFile, "rw").getChannel();
                flagLock = flagChannel.tryLock();

                if (!flagLock.isShared()) {
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

                if (flagChannel != null) {
                    try {
                        flagChannel.close();
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
