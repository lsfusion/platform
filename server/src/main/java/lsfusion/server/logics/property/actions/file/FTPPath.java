package lsfusion.server.logics.property.actions.file;

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
}