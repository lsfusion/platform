package platform.base;

import java.io.*;

public class OSUtils {

    public static String getResourcePath(String resource, String path, Class<?> cls, boolean overwrite, boolean appendPath) throws IOException {

        File file = createUserFile(appendPath ? ClassUtils.resolveName(cls, path + resource) : resource);
        if (overwrite || !file.exists()) { // пока сделаем так, хотя это не очень правильно, так как желательно все-таки обновлять dll'ки

            InputStream in = cls.getResourceAsStream(path + resource);

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

        return getResourcePath(libName + libExtension, path, cls, false, false); // будем считать, что в library зашифрована вер
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

        String userDirPath = System.getProperty("user.home", "") + "/.fusion" ;
        File userDir = new File(userDirPath);
        if (!userDir.exists()) {
            userDir.mkdirs(); // создаем каталог, на всякий случай, чтобы каждому не приходилось его создавать
        }

        return userDir;
    }

    public static File createUserFile(String fileName) {
        File userFile = new File(getUserDir().getAbsolutePath() + "/" + fileName);
        File userDir = userFile.getParentFile();
        if (!userDir.exists())
            userDir.mkdirs();
        return userFile;
    }
}
