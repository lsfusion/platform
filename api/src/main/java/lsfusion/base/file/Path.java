package lsfusion.base.file;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Path {
    public String type;
    public String path;

    public Path(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public static Path parsePath(String sourcePath) {
        return parsePath(sourcePath, false);
    }

    public static Path parsePath(String sourcePath, boolean read) {
        String pattern = read ? "(file|ftp|sftp|http|https|jdbc|mdb):(?:/{1,2})?(.*)" : "(file|ftp|sftp):(?:/{1,2})?(.*)";
        String[] types = read ? new String[]{"file:", "ftp:", "sftp:", "http:", "https:", "jdbc:", "mdb:"} : new String[]{"file:", "ftp:", "sftp:"};

        if (!StringUtils.startsWithAny(sourcePath, types)) {
            sourcePath = "file:" + sourcePath;
        }
        Matcher m = Pattern.compile(pattern).matcher(sourcePath);
        if (m.matches()) {
            return new Path(m.group(1).toLowerCase(), m.group(2));
        } else {
            throw new RuntimeException("Unsupported path: " + sourcePath);
        }
    }
}