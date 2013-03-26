package platform.base;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.interop.remote.ServerSocketFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;

import static platform.base.ApiResourceBundle.getString;

public class SystemUtils {
    private static final Logger logger = Logger.getLogger(SystemUtils.class);

    public static String getResourcePath(String resource, String path, Class<?> cls, boolean overwrite, boolean appendPath) throws IOException {

        File file = getUserFile(appendPath ? resolveName(cls, path + resource) : resource);
        if (overwrite || !file.exists()) { // пока сделаем так, хотя это не очень правильно, так как желательно все-таки обновлять dll'ки

            InputStream in = cls.getResourceAsStream(path + resource);
            if (in == null)
                throw new FileNotFoundException("File " + file.getName() + " not found");
            FileOutputStream out = new FileOutputStream(file);

            byte[] b = new byte[4096];
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
            }
            in.close();
            out.close();
        }
        return file.getAbsolutePath();
    }

    public static String getLibraryPath(String libName, String path, Class<?> cls) throws IOException {

        String system = System.getProperty("os.name");
        String libExtension =
                "Linux".equals(system) ? ".so" : ".dll";
        String libPath =
                "Linux".equals(system) ? "ux" : "win";
        libPath += is64Arch() ? "64" : "32";

        return getResourcePath(libName + libExtension, path + libPath + '/', cls, false, false); // будем считать, что в library зашифрована вер
    }

    public static void saveCurrentDirectory(File lastFolder) {
        if (lastFolder != null) {
            Preferences preferences = Preferences.userNodeForPackage(SystemUtils.class);
            preferences.put("LATEST_DIRECTORY", lastFolder.getAbsolutePath());
        }
    }

    public static File loadCurrentDirectory() {
        Preferences preferences = Preferences.userNodeForPackage(SystemUtils.class);
        return new File(preferences.get("LATEST_DIRECTORY", ""));
    }

    public static boolean is64Arch() {
        String osArch = System.getProperty("os.arch");
        return osArch != null && osArch.contains("64");
    }

    public static File getLocalClassesDir() {
        return getUserDir();
    }

    public static void loadClass(String className, String path, Class<?> cls) throws IOException {
        getResourcePath(className + ".class", path, cls, true, true); // для класса обновляем, поскольку иначе изменения не будут обновляться
    }

    public static void loadLibrary(String libName, String path, Class<?> cls) throws IOException {
        System.load(getLibraryPath(libName, path, cls));
    }

    public static File getUserDir() {

        String userDirPath = System.getProperty("user.home", "") + "/.fusion";
        File userDir = new File(userDirPath);
        if (!userDir.exists()) {
            userDir.mkdirs(); // создаем каталог, на всякий случай, чтобы каждому не приходилось его создавать
        }

        return userDir;
    }

    public static File getUserFile(String fileName) {
        return getUserFile(fileName, true);
    }

    public static File getUserFile(String fileName, boolean makeDir) {
        File userFile = new File(getUserDir().getAbsolutePath() + "/" + fileName);
        File userDir = userFile.getParentFile();
        if (makeDir && !userDir.exists())
            userDir.mkdirs();
        return userFile;
    }

    public static String getLocalHostName() {
        String name = null;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()));
            name = reader.readLine();
        } catch (IOException e) {
        }
        if (name == null || name.trim().isEmpty()) {
            try {
                InetAddress address = InetAddress.getLocalHost();
                name = address.getCanonicalHostName();
            } catch (UnknownHostException uhe) {
            }

            if (name == null || name.trim().isEmpty()) {
                name = "localhost";
            }
        }
        return name;
    }

    public static String getLocalHostIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignore) {
        }
        return null;
    }

    public static String convertPath(String path, Boolean convertFirst) {
        if (path.startsWith("\\") && convertFirst)
            return "\\" + path.substring(2, path.length()).replace("\\", "/");
        else
            return path.replace("\\", "/");
    }

    public static TimeZone getCurrentTimeZone() {
        return Calendar.getInstance().getTimeZone();
    }

    public static String resolveName(Class<?> c, String name) {
        if (name == null) {
            return name;
        }
        if (!name.startsWith("/")) {
            while (c.isArray()) {
                c = c.getComponentType();
            }
            String baseName = c.getName();
            int index = baseName.lastIndexOf('.');
            if (index != -1) {
                name = baseName.substring(0, index).replace('.', '/')
                    +"/"+name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    public static void initRMICompressedSocketFactory() throws IOException {
        if (RMISocketFactory.getSocketFactory() == null) {
            RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
                public boolean failure(Exception ex) {
                    logger.error(getString("exceptions.rmi.error") + " ", ex);
                    return true;
                }
            });

            RMISocketFactory.setSocketFactory(new ServerSocketFactory());
        }
    }

    public static void killRmiThread() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("RMI Reaper".equals(t.getName())) {
                t.interrupt();
            }
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    public static String getVMInfo() {
        Long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        Long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        Long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        Integer processors = Runtime.getRuntime().availableProcessors();
        return "Processors: " + processors + "\n" +
                "Free Memory: " + freeMemory + " MB\n" +
                "Total Memory: " + totalMemory + " MB\n" +
                "Max Memory: " + maxMemory + " MB";
    }

    public static String getRevision() {
        return getRevision("/platform/server");
    }

    public static String getRevision(String basePackage) {
        String revision = null;
        InputStream manifestStream = SystemUtils.class.getResourceAsStream(basePackage + "/../../META-INF/MANIFEST.MF");
        try {
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                revision = attributes.getValue("SCM-Version");
            }
        } catch (IOException ignore) {
        }
        return revision;
    }
}
