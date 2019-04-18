package lsfusion.base.file;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPPath {
    public String username;
    public String password;
    public String charset;
    public String server;
    public Integer port;
    public String remoteFile;
    public boolean passiveMode;

    public FTPPath(String username, String password, String charset, String server, Integer port, String remoteFile, boolean passiveMode) {
        this.username = username;
        this.password = password;
        this.charset = charset;
        this.server = server;
        this.port = port;
        this.remoteFile = remoteFile;
        this.passiveMode = passiveMode;
    }

    public static FTPPath parseFTPPath(String path) {
        return parseFTPPath(path, 21);
    }

    public static FTPPath parseSFTPPath(String path) {
        return parseFTPPath(path, 22);
    }

    private static FTPPath parseFTPPath(String path, Integer defaultPort) {
        /*username:password;charset@host:port/path_to_file?passivemode=false*/
        Pattern connectionStringPattern = Pattern.compile("(.*):([^;]*)(?:;(.*))?@([^/:]*)(?::([^/]+))?(?:/([^?]*))?(?:\\?(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1);
            String password = connectionStringMatcher.group(2);
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4);
            Integer port = connectionStringMatcher.group(5) == null ? defaultPort : Integer.parseInt(connectionStringMatcher.group(5));
            String remoteFile = connectionStringMatcher.group(6);
            List<NameValuePair> extraParams = URLEncodedUtils.parse(connectionStringMatcher.group(7), charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8);
            boolean passiveMode = isPassiveMode(extraParams);
            return new FTPPath(username, password, charset, server, port, remoteFile, passiveMode);
        } else {
            throw new RuntimeException("Incorrect ftp url. Please use format: ftp(s)://username:password;charset@host:port/path_to_file?passivemode=false");
        }
    }

    private static boolean isPassiveMode(List<NameValuePair> queryParams) {
        String result = getParameterValue(queryParams, "passivemode");
        return result == null || result.equals("true");
    }

    private static String getParameterValue(List<NameValuePair> queryParams, String key) {
        List<String> values = new ArrayList<>();
        for(NameValuePair queryParam : queryParams) {
            if(queryParam.getName().equalsIgnoreCase(key))
                values.add(queryParam.getValue());
        }
        return values.isEmpty() ? null : values.get(0);
    }
}