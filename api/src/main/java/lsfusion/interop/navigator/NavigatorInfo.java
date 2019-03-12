package lsfusion.interop.navigator;

import lsfusion.interop.session.SessionInfo;

import java.io.Serializable;

public class NavigatorInfo implements Serializable {
    
    public final SessionInfo session;

    public final String osVersion;
    public final String processor;
    public final String architecture;
    public final Integer cores;
    public final Integer physicalMemory;
    public final Integer totalMemory;
    public final Integer maximumMemory;
    public final Integer freeMemory;
    public final String javaVersion;
    public final String screenSize;
    public final String platformVersion;
    public final Integer apiVersion;

    public NavigatorInfo(SessionInfo session, String osVersion, String processor,
                         String architecture, Integer cores, Integer physicalMemory, Integer totalMemory, Integer maximumMemory,
                         Integer freeMemory, String javaVersion, String screenSize, String platformVersion, Integer apiVersion) {
        this.session = session;
        
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
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
    }
}