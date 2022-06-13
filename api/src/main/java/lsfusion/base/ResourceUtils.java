package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.base.lambda.EFunction;
import lsfusion.interop.action.ClientWebAction;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        retval.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int p1 = BaseUtils.countOccurrences(o1, '/');
                int p2 = BaseUtils.countOccurrences(o2, '/');
                if (p1 < p2) return -1;
                if (p1 > p2) return 1;
                return o1.compareTo(o2);
            }
        });

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

    public static Map<String, String> getSourceToBuildDirs() {
        Map<String, String> sourceBuildDirs = new HashMap<>();
        String[] buildDirs = {"target/classes", "out/production"};
        String[] srcDirs = {"src/main/resources", "src/main/lsfusion"};

        Arrays.stream(getClassPathElements())
                .map(FilenameUtils::separatorsToUnix)
                .forEach(unixClassPath -> Arrays.stream(buildDirs).filter(buildDir -> unixClassPath.contains(buildDir))
                        .forEach(buildDir -> Arrays.stream(srcDirs).forEach(srcDir -> {
                            Path path = Paths.get(unixClassPath.substring(0, unixClassPath.indexOf(buildDir)), srcDir);
                            if (path.toFile().exists())
                                sourceBuildDirs.put(path.toString(), unixClassPath);
                        })));

        return sourceBuildDirs;
    }

    private final static ConcurrentHashMap<String, Pair<List<String>, Map<String, String>>> cachedFoundResourcePathes = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Pair<String, String>> cachedFoundResourcesAsStrings = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Pair<RawFileData, String>> cachedFoundResourcesAsFileDatas = new ConcurrentHashMap<>();

    public static <T> T findResourceAs(String resourcePath, EFunction<InputStream, T, IOException> result, ConcurrentHashMap<String, Pair<T, String>> cacheMap, boolean nullIfNotExists, boolean multipleUsages, Result<String> fullPath, String optimisticFolder) {
        if(multipleUsages && !inDevMode) { // caching, more to save memory, rather than improve speed
            Pair<T, String> cachedResult = cacheMap.get(resourcePath);
            if(cachedResult == null) {
                Result<String> cacheFullPath = new Result<>();
                cachedResult = new Pair<>(calcFindResourceAs(resourcePath, result, nullIfNotExists, multipleUsages, cacheFullPath, optimisticFolder), cacheFullPath.result);
                cacheMap.put(resourcePath, cachedResult);
            }
            if(fullPath != null)
                fullPath.set(cachedResult.second);
            return cachedResult.first;
        }

        return calcFindResourceAs(resourcePath, result, nullIfNotExists, multipleUsages, fullPath, optimisticFolder);
    }

    public static <T> T calcFindResourceAs(String resourcePath, EFunction<InputStream, T, IOException> result, boolean nullIfNotExists, boolean multipleUsages, Result<String> fullPath, String optimisticFolder) {
        resourcePath = findResourcePath(resourcePath, nullIfNotExists, multipleUsages, optimisticFolder);
        if (resourcePath == null)
            return null;
        if(fullPath != null)
            fullPath.set(resourcePath.substring(1));
        try {
            return result.apply(ResourceUtils.getResourceAsStream(resourcePath, multipleUsages));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String findResourceAsString(String resourcePath, boolean nullIfNotExists, boolean multipleUsages, Result<String> fullPath, String optimisticFolder) {
        return findResourceAs(resourcePath, stream -> IOUtils.readStreamToString(stream, "UTF-8"), cachedFoundResourcesAsStrings, nullIfNotExists, multipleUsages, fullPath, optimisticFolder);
    }
    public static RawFileData findResourceAsFileData(String resourcePath, boolean nullIfNotExists, boolean multipleUsages, Result<String> fullPath, String optimisticFolder) {
        return findResourceAs(resourcePath, RawFileData::new, cachedFoundResourcesAsFileDatas, nullIfNotExists, multipleUsages, fullPath, optimisticFolder);
    }

    public static URL getResource(String path) {
        return ResourceUtils.class.getResource(path);
    }

    public static InputStream getResourceAsStream(String path, boolean multipleUsages) {
        return ResourceUtils.class.getResourceAsStream(path);
    }

    public static boolean inDevMode = false;
    public static String findResourcePath(String fileName, boolean nullIfNotExists, boolean multipleUsages, String optimisticFolder) {
        if(fileName.startsWith("/")) {
            //absolute path
            if(ResourceUtils.getResource(fileName) != null)
                return fileName;
        } else {
            // we can't use optimistic folder, since it will break the classpath precedence
//            if(optimisticFolder != null) {
//                String optimisticPath = "/" + optimisticFolder + "/" + fileName;
//                if(ResourceUtils.getResource(optimisticPath) != null)
//                    return optimisticPath;
//            }

            if(!(multipleUsages && inDevMode)) { // if we have "not init" read and we are not in devMode, ignore caches to have better DX
                boolean simpleFile = fileName.equals(BaseUtils.getFileNameAndExtension(fileName));

                String template = BaseUtils.replaceFileName(fileName, ".*", true);

                Pair<List<String>, Map<String, String>> cachedResources = cachedFoundResourcePathes.get(template);
                if(cachedResources == null) {
                    Pattern pattern = Pattern.compile(".*/" + template);
                    List<String> resources = ResourceUtils.getResources(pattern);
                    cachedResources = new Pair<>(resources, simpleFile ? BaseUtils.groupListFirst(BaseUtils::getFileNameAndExtension, resources) : null);
                    cachedFoundResourcePathes.put(template, cachedResources);
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
        if(!nullIfNotExists) {
            throw new RuntimeException(ApiResourceBundle.getString("exceptions.file.not.found", fileName));
        }
        return null;
    }
    public static void clearResourceCaches(boolean pathesChanged, boolean dataChanged, Predicate<String> checkExtension) {
        if(pathesChanged)
            clearResourceCaches(cachedFoundResourcePathes, checkExtension);

        if(dataChanged) {
            clearResourceCaches(cachedFoundResourcesAsStrings, checkExtension);
            clearResourceCaches(cachedFoundResourcesAsFileDatas, checkExtension);
        }
    }

    private static <T> void clearResourceCaches(ConcurrentHashMap<String, T> cache, Predicate<String> checkExtension) {
        cache.keySet().stream().filter(checkExtension).forEach(cache::remove);
    }

    public static String registerFont(ClientWebAction action) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font font = Font.createFont(Font.TRUETYPE_FONT, ((RawFileData) action.resource).getInputStream());
            ge.registerFont(font);
            return font.getFamily();
        } catch (FontFormatException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
}