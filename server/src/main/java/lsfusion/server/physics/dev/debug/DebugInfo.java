package lsfusion.server.physics.dev.debug;

import lsfusion.base.Pair;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import sun.awt.OSInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DebugInfo {

    public static class DebugPoint {
        public final String moduleName;
        public final String command;
        public final int line;
        public final int offset;
        public boolean isInsideNonEnabledMeta;
        
        public String topName;
        public LocalizedString topCaption;
        
        public DebugPoint(String moduleName, String path, int line, int offset, boolean isInsideNonEnabledMeta, String topName, LocalizedString topCaption) {
            this.moduleName = moduleName;
            String userDir = System.getProperty("user.dir");
            String binPath = System.getProperty("idea.bin.path");

            Path srcPath = Paths.get(userDir, "src/main/lsfusion/");
            Path customPath = !Files.exists(srcPath) ? Paths.get(userDir, path) : Paths.get(srcPath.toString(), path);

            if (Files.exists(customPath) && binPath != null) {
                String ideaRunCommand = null;
                boolean addQuotes = false;
                if (OSInfo.getOSType().equals(OSInfo.OSType.LINUX)) {
                    ideaRunCommand = binPath + "/idea.sh";
                } else if (OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS)) {
                    ideaRunCommand = binPath + (Files.exists(Paths.get(binPath, "idea64.exe")) ? "/idea64.exe" : "/idea.exe");
                    addQuotes = true;
                } else if (OSInfo.getOSType().equals(OSInfo.OSType.MACOSX)) {
                    ideaRunCommand = "/idea";
                }

                this.command = ideaRunCommand != null ? addQuotes ? "\"" + ideaRunCommand + "\"" + " --line " + (line + 1) + " " + "\"" + customPath + "\"" :
                        ideaRunCommand + " --line " + (line + 1) + " " + customPath : null;

            } else {
                this.command = null;
            }

            this.line = line;
            this.offset = offset;
            this.isInsideNonEnabledMeta = isInsideNonEnabledMeta;
            
            this.topName = topName;
            this.topCaption = topCaption;
        }
        
        public boolean needToCreateDelegate() {
            return !isInsideNonEnabledMeta;
        }
        
        @Override
        public String toString() {
            final String openedMetaCharacter = "\u2195";
            String openedMetaSuffix = isInsideNonEnabledMeta ? openedMetaCharacter : "";
            return moduleName + "(" + (line + 1) + ":" + (offset + 1) + openedMetaSuffix + ")";
        }
    }

    private DebugPoint point;
    
    private boolean needToCreateDelegate = true;
    private Integer debuggerLine = null;
    private Integer debuggerOffset = null;
    
    protected DebugInfo(DebugPoint debugPoint) {
        this.point = debugPoint;
    }
    
    protected DebugInfo(DebugPoint debugPoint, Integer debuggerLine, Integer debuggerOffset) {
        this(debugPoint);
        this.debuggerLine = debuggerLine;
        this.debuggerOffset = debuggerOffset;
    }

    public Pair<String, Integer> getDebuggerModuleLine() {
        return new Pair<>(point.moduleName, getDebuggerLine());
    }

    public String getDebuggerMethodName(boolean firstInLine) {
        return "action_" + getDebuggerLine() + (firstInLine ? "" : "_" + getDebuggerOffset());
    }

    public int getDebuggerLine() {
        return debuggerLine == null ? point.line : debuggerLine;
    }
    
    public int getDebuggerOffset() {
        return debuggerOffset == null ? point.offset : debuggerOffset;
    }   
    
    @Override
    public String toString() {
        return point.toString();
    }

    public void setNeedToCreateDelegate(boolean needToCreateDelegate) {
        this.needToCreateDelegate = needToCreateDelegate;
    }

    public boolean needToCreateDelegate() {
        return needToCreateDelegate && point.needToCreateDelegate();    
    } 
    
    public String getModuleName() {
        return point.moduleName;
    }

    public String getPath() {
        return point.command;
    }

    public DebugPoint getPoint() {
        return point;
    }
    
    public String getTopName() {
        return point.topName;
    }
    
    public LocalizedString getTopCaption() {
        return point.topCaption; 
    }
}
