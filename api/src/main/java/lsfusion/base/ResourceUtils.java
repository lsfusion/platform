package lsfusion.base;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardWatchEventKinds.*;
import static lsfusion.base.BaseUtils.isRedundantString;

public class ResourceUtils {

    public static Collection<String> getResources(final List<Pattern> patterns) {
        final ArrayList<String> retval = new ArrayList<>();
        for (final String element : getClassPathElements()) {
            if (!isRedundantString(element)) {
                assert !element.endsWith("/"); // нужен для другого использования getClassPathElements
                retval.addAll(getResources(element, patterns));
            }
        }
        return retval;
    }
    
    public static Collection<String> getResources(final Pattern pattern) {
        return getResources(Collections.singletonList(pattern));
    }

    private static Collection<String> getResources(final String element, final List<Pattern> patterns) {
        final ArrayList<String> retval = new ArrayList<>();

        if (element.endsWith("*")) {
            //java поддерживает возможность задавать в classpath все jar-ки в директории
            File dir = new File(element.substring(0, element.length() - 1));
            if (dir.isDirectory()) {
                File[] childFiles = dir.listFiles();
                if (childFiles != null) {
                    for (File childFile : childFiles) {
                        if (childFile.getName().endsWith(".jar")) {
                            retval.addAll(getResourcesFromJarFile(childFile, patterns));
                        }
                    }
                }
            }
        } else {
            final File file = new File(element);
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, "/", patterns));
            } else {
                retval.addAll(getResourcesFromJarFile(file, patterns));
            }
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(final File file, final List<Pattern> patterns) {
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
            fillResourcesResult("/" + fileName, patterns, retval);
        }
        try {
            zf.close();
        } catch (final IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(final File directory, final String relativePath, final List<Pattern> patterns) {
        final ArrayList<String> result = new ArrayList<>();

        final File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (final File file : fileList) {
                if (file.isDirectory()) {
                    result.addAll(getResourcesFromDirectory(file, relativePath + file.getName() +"/", patterns));
                } else {
                    final String fileName = relativePath + file.getName(); // SystemUtils.convertPath(file.getCanonicalPath(), true);
                    fillResourcesResult(fileName, patterns, result);
                }
            }
        }
        return result;
    }

    private static void fillResourcesResult(String fileName, List<Pattern> patterns, List<String> result) {
        assert fileName.startsWith("/");
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                result.add(matcher.groupCount() > 0 ? matcher.group(1) : fileName);
                break;
            }
        }
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
    
    public static ImageIcon readImage(String imagePath) {
        URL resource = ResourceUtils.class.getResource("/images/" + imagePath);
        return resource != null ? new ImageIcon(resource) : null;
    }
}  