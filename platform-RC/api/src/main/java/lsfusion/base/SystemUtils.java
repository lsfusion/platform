package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import org.apache.commons.io.FileUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;

public class SystemUtils {
    /**
     * The prefix String for all Windows OS.
     */
    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    /**
     * <p>The <code>line.separator</code> System Property. Line separator
     * (<code>&quot;\n&quot;</code> on UNIX).</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String LINE_SEPARATOR = getSystemProperty("line.separator");

    /**
     * <p>The <code>os.arch</code> System Property. Operating system architecture.</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_ARCH = getSystemProperty("os.arch");

    /**
     * <p>The <code>os.name</code> System Property. Operating system name.</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_NAME = getSystemProperty("os.name");

    /**
     * <p>The <code>os.version</code> System Property. Operating system version.</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_VERSION = getSystemProperty("os.version");

    /**
     * <p>The <code>path.separator</code> System Property. Path separator
     * (<code>&quot;:&quot;</code> on UNIX).</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String PATH_SEPARATOR = getSystemProperty("path.separator");

    /**
     * <p>Is <code>true</code> if this is AIX.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_AIX = getOSMatches("AIX");

    /**
     * <p>Is <code>true</code> if this is HP-UX.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_HP_UX = getOSMatches("HP-UX");

    /**
     * <p>Is <code>true</code> if this is Irix.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_IRIX = getOSMatches("Irix");

    /**
     * <p>Is <code>true</code> if this is Linux.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_LINUX = getOSMatches("Linux") || getOSMatches("LINUX");

    /**
     * <p>Is <code>true</code> if this is Mac.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_MAC = getOSMatches("Mac");

    /**
     * <p>Is <code>true</code> if this is Mac.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_MAC_OSX = getOSMatches("Mac OS X");

    /**
     * <p>Is <code>true</code> if this is OS/2.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_OS2 = getOSMatches("OS/2");

    /**
     * <p>Is <code>true</code> if this is Solaris.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_SOLARIS = getOSMatches("Solaris");

    /**
     * <p>Is <code>true</code> if this is SunOS.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_SUN_OS = getOSMatches("SunOS");

    /**
     * <p>Is <code>true</code> if this is a POSIX compilant system,
     * as in any of AIX, HP-UX, Irix, Linux, MacOSX, Solaris or SUN OS.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.1
     */
    public static final boolean IS_OS_UNIX =
            IS_OS_AIX || IS_OS_HP_UX || IS_OS_IRIX || IS_OS_LINUX ||
            IS_OS_MAC_OSX || IS_OS_SOLARIS || IS_OS_SUN_OS;

    /**
     * <p>Is <code>true</code> if this is Windows.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS = getOSMatches(OS_NAME_WINDOWS_PREFIX);

    /**
     * <p>Is <code>true</code> if this is Windows 2000.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_2000 = getOSMatches(OS_NAME_WINDOWS_PREFIX, "5.0");

    /**
     * <p>Is <code>true</code> if this is Windows 95.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_95 = getOSMatches(OS_NAME_WINDOWS_PREFIX + " 9", "4.0");
    // JDK 1.2 running on Windows98 returns 'Windows 95', hence the above

    /**
     * <p>Is <code>true</code> if this is Windows 98.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_98 = getOSMatches(OS_NAME_WINDOWS_PREFIX + " 9", "4.1");
    // JDK 1.2 running on Windows98 returns 'Windows 95', hence the above

    /**
     * <p>Is <code>true</code> if this is Windows ME.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_ME = getOSMatches(OS_NAME_WINDOWS_PREFIX, "4.9");
    // JDK 1.2 running on WindowsME may return 'Windows 95', hence the above

    /**
     * <p>Is <code>true</code> if this is Windows NT.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_NT = getOSMatches(OS_NAME_WINDOWS_PREFIX + " NT");
    // Windows 2000 returns 'Windows 2000' but may suffer from same JDK1.2 problem

    /**
     * <p>Is <code>true</code> if this is Windows XP.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_XP = getOSMatches(OS_NAME_WINDOWS_PREFIX, "5.1");

    //-----------------------------------------------------------------------
    /**
     * <p>Is <code>true</code> if this is Windows Vista.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.4
     */
    public static final boolean IS_OS_WINDOWS_VISTA = getOSMatches(OS_NAME_WINDOWS_PREFIX, "6.0");

    /**
     * <p>Is <code>true</code> if this is Windows 7.</p>
     *
     * <p>The field will return <code>false</code> if <code>OS_NAME</code> is
     * <code>null</code>.</p>
     *
     * @since 2.5
     */
    public static final boolean IS_OS_WINDOWS_7 = getOSMatches(OS_NAME_WINDOWS_PREFIX, "6.1");

    /**
     * <p>Decides if the operating system matches.</p>
     *
     * @param osNamePrefix  the prefix for the os name
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getOSMatches(String osNamePrefix) {
        if (OS_NAME == null) {
            return false;
        }
        return OS_NAME.startsWith(osNamePrefix);
    }

    /**
     * <p>Decides if the operating system matches.</p>
     *
     * @param osNamePrefix  the prefix for the os name
     * @param osVersionPrefix  the prefix for the version
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getOSMatches(String osNamePrefix, String osVersionPrefix) {
        if (OS_NAME == null || OS_VERSION == null) {
            return false;
        }
        return OS_NAME.startsWith(osNamePrefix) && OS_VERSION.startsWith(osVersionPrefix);
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Gets a System property, defaulting to <code>null</code> if the property
     * cannot be read.</p>
     *
     * <p>If a <code>SecurityException</code> is caught, the return
     * value is <code>null</code> and a message is written to <code>System.err</code>.</p>
     *
     * @param property the system property name
     * @return the system property value or <code>null</code> if a security problem occurs
     */
    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            System.err.println(
                    "Caught a SecurityException reading the system property '" + property
                    + "'; the SystemUtils property value will default to null."
            );
            return null;
        }
    }

    public static String getResourcePath(String resource, String path, Class<?> cls, boolean overwrite, boolean appendPath) throws IOException {
        File file = getUserFile(appendPath ? resolveName(cls, path + resource) : resource);
        File newFile = getFile(file, resource, path, cls, overwrite);
        if (overwrite || !file.exists() || file.length() != newFile.length())
            FileCopyUtils.copy(newFile, file);
        if(!newFile.delete())
            newFile.deleteOnExit();
        return file.getAbsolutePath();
    }

    private static File getFile(File oldFile, String resource, String path, Class<?> cls, boolean overwrite) throws IOException {
        File newFile = File.createTempFile("temp", ".tmp");
        InputStream in = cls.getResourceAsStream(path + resource);
        if (in == null) {
            if (overwrite || !oldFile.exists())
                throw new FileNotFoundException("File " + oldFile.getName() + " not found");
        } else {
            FileOutputStream out = new FileOutputStream(newFile);
            byte[] b = new byte[4096];
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
            }
            in.close();
            out.close();
        }
        return newFile;
    }

    public static String getLibraryPath(String libName, String path, Class<?> cls) throws IOException {

        String libExtension = IS_OS_WINDOWS ? ".dll" : ".so";
        String libPath = (IS_OS_WINDOWS ? "win" : "ux") + (is64Arch() ? "64" : "32");

        return getResourcePath(libName + libExtension, path + libPath + '/', cls, false, false); // будем считать, что в library зашифрована вер
    }

    public static String getExePath(String exeName, String path, Class<?> cls) throws IOException {
        assert IS_OS_WINDOWS;
        return getResourcePath(exeName + ".exe", path, cls, true, false); // будем считать, что в library зашифрована вер
    }

    public static void loadLibrary(String libName, String path, Class<?> cls) throws IOException {
        System.load(getLibraryPath(libName, path, cls));
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

    public static void writeUserFile(String filename, byte[] bytes) throws IOException {
        File file = getUserFile(filename);
        FileUtils.writeByteArrayToFile(file, bytes);
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
    
    public static String getJavaVersion() {
        return getSystemProperty("java.version");
    }
    
    public static String getJavaSpecificationVersionString() {
        return getSystemProperty("java.specification.version");   
    }
    
    public static Double getJavaSpecificationVersion() {
        try {
            return Double.parseDouble(getJavaSpecificationVersionString());
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
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
        Integer processors = getAvailableProcessors();
        return "Processors: " + processors + "\n" +
                "Free Memory: " + freeMemory + " MB\n" +
                "Total Memory: " + totalMemory + " MB\n" +
                "Max Memory: " + maxMemory + " MB";
    }
    
    private static int availableProcessors = -1;
    
    public static int getAvailableProcessors() {
        if (availableProcessors == -1) {
            availableProcessors = Runtime.getRuntime().availableProcessors();
        }
        return availableProcessors;
    }

    public static String getRevision() {
        String revision = null;
        InputStream manifestStream = SystemUtils.class.getResourceAsStream("/META-INF/MANIFEST.MF");
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

    public static boolean isPortAvailable(int port) {
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
        } catch (Exception e) {
            // Getting exception means the port is not used by other applications
            return true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return false;
    }
    
    private static char[] ids = new char[] {
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            '0','1','2','3','4','5','6', '7', '8', '9', '_', 'x'
    };
    
    private static String generateIDForHash(byte[] hash) {
        assert ids.length == 64;
        
        int bytes = 0;
        byte currentByte = hash[bytes];
        int bit = 0;
        char[] result = new char[hash.length * 8 / 6 + 1];
        int num = 0;
        while(bytes < hash.length) {
            byte symb = 0;
            
            for(int i=0;i<6;i++) {
                symb = (byte) ((symb << 1) + (currentByte & 1));
                currentByte = (byte) (currentByte >> 1);
                bit++;
                if(bit == 8) {
                    bit = 0;
                    if(++bytes >= hash.length)
                        break;
                    currentByte = hash[bytes];
                }
            }
            if(num==0 && symb >= 52)
                symb = 0;
            result[num++] = ids[symb];
        }
        return new String(result, 0, num);
    }
    
    public static String generateID(byte[] array) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(array);
            byte[] digest = messageDigest.digest();
            return generateIDForHash(digest);
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String generateID(BinarySerializable object) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            object.write(new DataOutputStream(stream));
            return SystemUtils.generateID(stream.toByteArray());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <K extends BinarySerializable> void write(DataOutputStream stream, ImList<K> list) throws IOException {
        for(K element : list)
            element.write(stream);
    }
}
