package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientWebAction;
import lsfusion.interop.action.RunCommandActionResult;
import lsfusion.interop.form.remote.serialization.BinarySerializable;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.prefs.Preferences;

import static lsfusion.base.BaseUtils.isRedundantString;

public class SystemUtils {

    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    public static final String OS_NAME = getSystemProperty("os.name");

    public static final boolean IS_OS_WINDOWS = getOSMatches(OS_NAME_WINDOWS_PREFIX);

    private static boolean getOSMatches(String osNamePrefix) {
        if (OS_NAME == null) {
            return false;
        }
        return OS_NAME.startsWith(osNamePrefix);
    }

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
            FileUtils.copyFile(newFile, file);
        BaseUtils.safeDelete(newFile);
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
            name = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream())).readLine();
            //Windows allows cyrillic symbols in hostname. But all of them are replaced with "?" when reading via Runtime exec
            //System.getEnv supports cyrillic symbols, but returns hostname uppercase
            if (name != null && name.contains("?")) {
                name = System.getenv("COMPUTERNAME");
            }
        } catch (IOException ignored) {
        }

        if (isRedundantString(name)) {
            try {
                InetAddress address = InetAddress.getLocalHost();
                name = address.getCanonicalHostName();
            } catch (UnknownHostException ignored) {
            }
        }

        return isRedundantString(name) ? "localhost" : name;
    }

    public static String getLocalHostIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignore) {
        }
        return null;
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
        Integer processors = Runtime.getRuntime().availableProcessors();
        String javaVesion = SystemUtils.getJavaVersion();
        return "Processors: " + processors + "\n" +
                "Free Memory: " + freeMemory + " MB\n" +
                "Total Memory: " + totalMemory + " MB\n" +
                "Max Memory: " + maxMemory + " MB\n" +
                "Java version: " + javaVesion;
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

    public static void registerLibrary(ClientWebAction action) {
        try {
            byte[] serverBytes = ((RawFileData) action.resource).getBytes();
            File clientFile = getUserFile(action.resourceName);

            if (!clientFile.exists() || !Arrays.equals(serverBytes, FileUtils.readFileToByteArray(clientFile))) {
                writeUserFile(action.resourceName, serverBytes);
            }

            String libraryPath = clientFile.getParentFile().getAbsolutePath();
            setLibraryPath(libraryPath, "jna.library.path");
            setLibraryPath(libraryPath, "java.library.path");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void setLibraryPath(String path, String property) {
        String libraryPath = System.getProperty(property);
        if (libraryPath == null) {
            System.setProperty(property, path);
        } else if (!libraryPath.contains(path)) {
            System.setProperty(property, path + ";" + libraryPath);
        }
    }

    public static RunCommandActionResult runCmd(String command, String directory, boolean wait) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process p = directory != null ? runtime.exec(command, null, new File(directory)) : runtime.exec(command);
        RunCommandActionResult result = null;
        if (wait) {
            try {
                String cmdOut = readInputStreamToString(p.getInputStream());
                String cmdErr = readInputStreamToString(p.getErrorStream());

                p.waitFor();

                int exitValue = p.exitValue();
                result = new RunCommandActionResult(cmdOut, cmdErr, exitValue);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private static String readInputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) { //
            StringBuilder errS = new StringBuilder();
            byte[] b = new byte[1024];
            while (bufferedInputStream.read(b) != -1) {
                errS.append(new String(b, SystemUtils.IS_OS_WINDOWS ?  "cp866" : "utf-8").trim()).append("\n");
            }
            return BaseUtils.trimToNull(errS.toString());
        }
    }
}
