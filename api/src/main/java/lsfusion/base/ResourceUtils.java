package lsfusion.base;

import com.google.common.base.Throwables;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;
import static lsfusion.base.BaseUtils.isRedundantString;

public class ResourceUtils {

    public static Collection<String> getResources(final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<>();
        for (final String element : getClassPathElements()) {
            if (!isRedundantString(element)) {
                retval.addAll(getResources(element, pattern));
            }
        }
        return retval;
    }

    private static Collection<String> getResources(final String element, final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<>();

        if (element.endsWith("*")) {
            //java поддерживает возможность задавать в classpath все jar-ки в директории
            File dir = new File(element.substring(0, element.length() - 1));
            if (dir.isDirectory()) {
                File[] childFiles = dir.listFiles();
                if (childFiles != null) {
                    for (File childFile : childFiles) {
                        if (childFile.getName().endsWith(".jar")) {
                            retval.addAll(getResourcesFromJarFile(childFile, pattern));
                        }
                    }
                }
            }
        } else {
            final File file = new File(element);
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, "", pattern));
            } else {
                retval.addAll(getResourcesFromJarFile(file, pattern));
            }
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(final File file, final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (final IOException e) {
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if (accept) {
                retval.add(fileName);
            }
        }
        try {
            zf.close();
        } catch (final IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(final File directory, final String relativePath, final Pattern pattern) {
        final ArrayList<String> result = new ArrayList<>();

        final File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (final File file : fileList) {
                if (file.isDirectory()) {
                    result.addAll(getResourcesFromDirectory(file, relativePath + (relativePath.isEmpty() ? "" : "/") + file.getName(), pattern));
                } else {
                    final String fileName = relativePath + (relativePath.isEmpty() ? "" : "/") + file.getName(); // SystemUtils.convertPath(file.getCanonicalPath(), true);
                    final boolean accept = pattern.matcher(fileName).matches();
                    if (accept) {
                        result.add(fileName);
                    }
                }
            }
        }
        return result;
    }

    public static String getClassPath() {
        return System.getProperty("java.class.path", ".");
    }

    public static String[] getClassPathElements() {
        return getClassPath().split(System.getProperty("path.separator"));
    }

    public static void watchClassPathFoldersForChange(final Runnable callback) {
        try {
            List<Path> paths = new ArrayList<>();
            for (final String element : getClassPathElements()) {
                if (!isRedundantString(element)) {
                    final Path path = Paths.get(element);
                    Boolean isFolder = (Boolean) Files.getAttribute(path, "basic:isDirectory", NOFOLLOW_LINKS);
                    if (isFolder) {
                        paths.add(path);
                    }
                }
            }
            watchPathsForChange(paths, callback);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
    
    public static void watchPathsForChange(final List<Path> paths, final Runnable callback) throws IOException {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        final WatchService watcher = FileSystems.getDefault().newWatchService();
        for (final Path path : paths) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        path.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE}, FILE_TREE);

                        WatchKey key;
                        while (true) {
                            try {
                                // wait for key to be signaled
                                key = watcher.take();
                            } catch (InterruptedException x) {
                                return;
                            }

                            for (WatchEvent<?> event : key.pollEvents()) {
                                WatchEvent.Kind<?> kind = event.kind();
                                if (kind == ENTRY_CREATE || kind == ENTRY_DELETE || kind == OVERFLOW) {
                                    callback.run();
                                }
                            }
                            key.reset();
                        }
                    } catch (IOException e) {
                        Throwables.propagate(e);
                    }
                }
            });
        }
    }
}  