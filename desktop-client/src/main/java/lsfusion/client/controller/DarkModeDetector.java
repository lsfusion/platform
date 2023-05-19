package lsfusion.client.controller;

import java.io.*;

//copied from https://harmoniccode.blogspot.com/2021/02/poor-mans-dark-mode-detection.html
public class DarkModeDetector {
    public enum OperatingSystem { WINDOWS, MACOS, LINUX, SOLARIS, NONE }

    private static final String REGQUERY_UTIL  = "reg query ";
    private static final String REGDWORD_TOKEN = "REG_DWORD";
    private static final String DARK_THEME_CMD = REGQUERY_UTIL + "\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\"" + " /v AppsUseLightTheme";

    public static final boolean isDarkMode() {
        switch(getOperatingSystem()) {
            case WINDOWS: return isWindowsDarkMode();
            case MACOS  : return isMacOsDarkMode();
            case LINUX  :
            case SOLARIS:
            default     : return false;
        }
    }

    public static final boolean isMacOsDarkMode() {
        try {
            boolean           isDarkMode = false;
            Runtime           runtime    = Runtime.getRuntime();
            Process           process    = runtime.exec("defaults read -g AppleInterfaceStyle");
            InputStreamReader isr        = new InputStreamReader(process.getInputStream());
            BufferedReader    rdr        = new BufferedReader(isr);
            String            line;
            while((line = rdr.readLine()) != null) {
                if (line.equals("Dark")) { isDarkMode = true; }
            }
            int rc = process.waitFor();  // Wait for the process to complete
            return 0 == rc && isDarkMode;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public static boolean isWindowsDarkMode() {
        try {
            Process      process = Runtime.getRuntime().exec(DARK_THEME_CMD);
            StreamReader reader  = new StreamReader(process.getInputStream());

            reader.start();
            process.waitFor();
            reader.join();

            String result = reader.getResult();
            int p = result.indexOf(REGDWORD_TOKEN);

            if (p == -1) { return false; }

            // 1 == Light Mode, 0 == Dark Mode
            String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
            return ((Integer.parseInt(temp.substring("0x".length()), 16))) == 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static final OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            return OperatingSystem.WINDOWS;
        } else if (os.indexOf("mac") >= 0) {
            return OperatingSystem.MACOS;
        } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            return OperatingSystem.LINUX;
        } else if (os.indexOf("sunos") >= 0) {
            return OperatingSystem.SOLARIS;
        } else {
            return OperatingSystem.NONE;
        }
    }


    // ******************** Internal Classes **********************************
    static class StreamReader extends Thread {
        private InputStream  is;
        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            } catch (IOException e) { ; }
        }

        String getResult() { return sw.toString(); }
    }
}