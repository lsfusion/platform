package lsfusion.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardWatchEventKinds.*;
import static lsfusion.base.BaseUtils.isRedundantString;

public class ResourceUtils {

    public static Collection<String> getResources(final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<>();
        for (final String element : getClassPathElements()) {
            if (!isRedundantString(element)) {
                assert !element.endsWith("/"); // нужен для другого использования getClassPathElements
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
                retval.addAll(getResourcesFromDirectory(file, "/", pattern));
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
            assert !fileName.startsWith("/");
            fillResourcesResult("/" + fileName, pattern, retval);
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
                    result.addAll(getResourcesFromDirectory(file, relativePath + file.getName() +"/", pattern));
                } else {
                    final String fileName = relativePath + file.getName(); // SystemUtils.convertPath(file.getCanonicalPath(), true);
                    fillResourcesResult(fileName, pattern, result);
                }
            }
        }
        return result;
    }

    private static void fillResourcesResult(String fileName, Pattern pattern, List<String> result) {
        assert fileName.startsWith("/");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches())
            result.add(matcher.groupCount() > 0 ? matcher.group(1) : fileName);
    }

    public static String getClassPath() {
        return System.getProperty("java.class.path", ".");
    }

    public static String[] getClassPathElements() {
        return getClassPath().split(System.getProperty("path.separator"));
    }

    public static void watchPathForChange(Path path, Runnable callback, Pattern pattern) throws IOException {
        final WatchService watchService = FileSystems.getDefault().newWatchService();
//        path.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE});
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });

        WatchKey key;
        while (true) {
            try {
                // wait for key to be signaled
                key = watchService.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == ENTRY_CREATE || kind == ENTRY_DELETE || kind == OVERFLOW) {
                    Path eventPath = (Path)event.context();
                    if(eventPath != null && pattern.matcher(eventPath.toString()).matches())
                        callback.run();
                }
            }
            key.reset();
        }
    }
}  