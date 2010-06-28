package platform.base;

import java.io.*;

public class OSUtils {

    public static void loadLibrary(String libName, String path, Class<?> cls) throws IOException {

        String system = System.getProperty("os.name");
        String libExtension =
           "Linux".equals(system) ? ".so" : ".dll";

        String myLibName = libName + libExtension;
        File file = new File(myLibName);
        if (!file.exists())
           file.createNewFile();
        InputStream in = cls.getResourceAsStream(path + myLibName);

        FileOutputStream out = new FileOutputStream(file);

        byte[] b = new byte[4096];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }

        in.close();
        out.close();

        System.loadLibrary(libName);
    }
}
