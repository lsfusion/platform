package lsfusion.base;

import java.io.Serializable;

public class NavigatorInfo implements Serializable{

    public String login;
    public String password;
    public int computer;
    public String remoteAddress;
    public String osVersion;
    public String processor;
    public String architecture;
    public Integer cores;
    public Integer physicalMemory;
    public Integer totalMemory;
    public Integer maximumMemory;
    public Integer freeMemory;
    public String javaVersion;
    public String screenSize;

    public NavigatorInfo(String login, String password, int computer, String remoteAddress, String osVersion, String processor,
                         String architecture, Integer cores, Integer physicalMemory, Integer totalMemory, Integer maximumMemory,
                         Integer freeMemory, String javaVersion, String screenSize) {
        this.login = login;
        this.password = password;
        this.computer = computer;
        this.remoteAddress = remoteAddress;
        this.osVersion = osVersion;
        this.processor = processor;
        this.architecture = architecture;
        this.cores = cores;
        this.physicalMemory = physicalMemory;
        this.totalMemory = totalMemory;
        this.maximumMemory = maximumMemory;
        this.freeMemory = freeMemory;
        this.javaVersion = javaVersion;
        this.screenSize = screenSize;
    }
}