package platform.base;

import java.io.*;

public class OSUtils {

    public static String getLibraryPath(String libName, String path, Class<?> cls) throws IOException {

        String system = System.getProperty("os.name");
        String libExtension =
           "Linux".equals(system) ? ".so" : ".dll";

        String myLibName = libName + libExtension;
        File file = createUserFile(myLibName);
        if (!file.exists()) { // пока сделаем так, хотя это не очень правильно, так как желательно все-таки обновлять lpsolve

            InputStream in = cls.getResourceAsStream(path + myLibName);

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

    public static void loadLibrary(String libName, String path, Class<?> cls) throws IOException {

        System.load(getLibraryPath(libName, path, cls));
    }

    public static File createUserFile(String fileName) {

        String userDirPath = System.getProperty("user.home", "") + "/.fusion" ;
        File userDir = new File(userDirPath);
        if (!userDir.exists()) {
            userDir.mkdirs(); // создаем каталог, на всякий случай, чтобы каждому не приходилось его создавать
        }

        return new File(userDirPath + "/" + fileName);
    }
}
