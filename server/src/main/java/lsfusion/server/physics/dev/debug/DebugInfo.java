package lsfusion.server.physics.dev.debug;

import lsfusion.base.Pair;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.net.URL;

public class DebugInfo {

    public static class DebugPoint {
        public final String moduleName;
        public final String path;
        public final int line;
        public final int offset;
        public boolean isInsideNonEnabledMeta;
        
        public String topName;
        public LocalizedString topCaption;
        
        public DebugPoint(String moduleName, String path, int line, int offset, boolean isInsideNonEnabledMeta, String topName, LocalizedString topCaption) {
            this.moduleName = moduleName;

            //path is used in the tooltips links to calculate absolute path to file.
            // If endpoint path located in a .jar, we don't need to show the link in tooltip
            URL resource = path != null ? getClass().getResource(path) : null;
            this.path = resource != null && !resource.getProtocol().contains("jar") ? path : null;

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
