package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardWatchEventKinds.*;
import static lsfusion.base.BaseUtils.isRedundantString;

public class ResourceUtils {

    public static List<String> getResources(final List<Pattern> patterns) {
        final ArrayList<String> retval = new ArrayList<>();
        for (final String element : getClassPathElements()) {
            if (!isRedundantString(element)) {
                assert !element.endsWith("/"); // нужен для другого использования getClassPathElements
                retval.addAll(getResources(element, patterns));
            }
        }
        return retval;
    }
    
    public static List<String> getResources(final Pattern pattern) {
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
            if (!ze.isDirectory()) {
                fillResourcesResult("/" + fileName, patterns, retval);
            }
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

    public static URL getResource(String path) {
        return ResourceUtils.class.getResource(path);
    }

    public static InputStream getResourceAsStream(String path) {
        return ResourceUtils.class.getResourceAsStream(path);
    }

    public static Path getTargetDir(String projDir) {
        Path targetDir = Paths.get(projDir, "target/classes");
        if(!Files.exists(targetDir)) { // if not maven then idea-project
            targetDir = Paths.get(projDir, "out/production/" + Paths.get(projDir).toFile().getName());
            if(!Files.exists(targetDir))
                targetDir = null;
        }
        return targetDir;
    }

    public static Path getTargetPath(String projDir, String fileName) {
        Path targetDir = getTargetDir(projDir);
        return targetDir == null ? Paths.get(projDir, fileName) : Paths.get(targetDir.toString(), fileName);
    }

    public static Path getCustomPath(String projDir, String fileName) {
        Path srcPath = getTargetDir(projDir) == null ? null : Paths.get(projDir, "src/main/lsfusion/");
        return srcPath == null || !Files.exists(srcPath) ? Paths.get(projDir, fileName) : Paths.get(srcPath.toString(), fileName);
    }

    public static String getFileParentDirectoryPath(String fileName) {
        URL resource = getResource(fileName);
        String fullPath = "";
        if(resource != null) {
            try {
                fullPath = FilenameUtils.separatorsToUnix(Paths.get(resource.toURI()).toString());
            } catch (URISyntaxException ignored) {
            }
        }

        assert fullPath.endsWith(fileName);
        return fullPath.substring(0, fullPath.length() - fileName.length());
    }

    public static Path getTargetClassesParentPath(String currentPath) {
        Path classesDir = Paths.get(currentPath);
        Path targetDir = classesDir.getParent();
        return equalName(classesDir, "classes") && equalName(targetDir, "target") ? Paths.get(currentPath, "../..") : null;
    }

    public static Path getOutProductionParentPath(String currentPath) {
        Path moduleDir = Paths.get(currentPath);
        Path productionDir = moduleDir.getParent();
        Path outDir = productionDir.getParent();
        return equalName(productionDir, "production") && equalName(outDir, "out") && equalName(outDir.getParent(), moduleDir.toFile().getName()) ? Paths.get(currentPath, "../../..") : null;
    }

    private static boolean equalName(Path path, String name) {
        return path.toFile().getName().equals(name);
    }

    public static List<String> findInClassPath(String endpoint) {
        return Arrays.stream(ResourceUtils.getClassPathElements())
                .filter(path -> Files.exists(Paths.get(path, endpoint)))
                .map(path -> FilenameUtils.separatorsToUnix(Paths.get(path, endpoint).toString()))
                .collect(Collectors.toList());
    }

    public static RawFileData findResourceAsFileData(String resourcePath, boolean checkExists, boolean cache, Result<String> fullPath, String optimisticFolder) {
        resourcePath = findResource(resourcePath, checkExists, cache, optimisticFolder);
        if (resourcePath == null)
            return null;
        if(fullPath != null)
            fullPath.set(resourcePath.substring(1)); // remov
        try {
            return new RawFileData(resourcePath, true);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private final static ConcurrentHashMap<String, Pair<List<String>, Map<String, String>>> cachedFoundResourses = new ConcurrentHashMap<>();
    public static String findResource(String fileName, boolean checkExists, boolean cache, String optimisticFolder) {
        if(fileName.startsWith("/")) {
            //absolute path
            if(!checkExists || ResourceUtils.getResource(fileName) != null)
                return fileName;
        } else {
            // we can't use optimistic folder, since it will break the classpath precedence
//            if(optimisticFolder != null) {
//                String optimisticPath = "/" + optimisticFolder + "/" + fileName;
//                if(ResourceUtils.getResource(optimisticPath) != null)
//                    return optimisticPath;
//            }

            if(cache) {
                boolean simpleFile = fileName.equals(BaseUtils.getFileNameAndExtension(fileName));

                String template = BaseUtils.replaceFileName(fileName, ".*", true);

                Pair<List<String>, Map<String, String>> cachedResources = cachedFoundResourses.get(template);
                if(cachedResources == null) {
                    Pattern pattern = Pattern.compile(".*/" + template);
                    List<String> resources = ResourceUtils.getResources(pattern);
                    cachedResources = new Pair<>(resources, simpleFile ? BaseUtils.groupListFirst(BaseUtils::getFileNameAndExtension, resources) : null);
                    cachedFoundResourses.put(template, cachedResources);
                }

                if(simpleFile)
                    return cachedResources.second.get(fileName);

                for (String entry : cachedResources.first)
                    if (entry.endsWith("/" + fileName))
                        return entry;
            } else {
                Pattern pattern = Pattern.compile(".*/" + fileName.replace(".", "\\."));
                List<String> result = ResourceUtils.getResources(pattern);
                if(!result.isEmpty())
                    return result.get(0);
            }
        }
        return null;
    }
    public static void clearResourceFileCaches(String extension) {
        Collection<String> cachedResourceKeys = new HashSet<>(cachedFoundResourses.keySet());
        for (String resource : cachedResourceKeys)
            if (resource.endsWith("." + extension))
                cachedFoundResourses.remove(resource);
    }
}