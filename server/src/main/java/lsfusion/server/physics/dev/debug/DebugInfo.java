package lsfusion.server.physics.dev.debug;

import lsfusion.base.Pair;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class DebugInfo {

    public static class DebugPoint {
        public final String moduleName;
        public final String path;
        public final int line;
        public final int offset;
        public boolean isInsideNonEnabledMeta;

        public final int globalLine; // as if all meta codes are expanded
        
        public String topName;
        public LocalizedString topCaption;
        
        public DebugPoint(String moduleName, String path, int line, int offset, boolean isInsideNonEnabledMeta, int globalLine, String topName, LocalizedString topCaption) {
            this.moduleName = moduleName;
            this.path = path;
            this.line = line;
            this.offset = offset;
            this.isInsideNonEnabledMeta = isInsideNonEnabledMeta;

            this.globalLine = globalLine;
            
            this.topName = topName;
            this.topCaption = topCaption;
        }
        
        public boolean needToCreateDelegate() {
            return !isInsideNonEnabledMeta;
        }

        public String getFullPath() {
            return path + getLineWithOffset();
        }
        
        @Override
        public String toString() {
            return moduleName + getLineWithOffset();
        }

        private String getLineWithOffset() {
            final String openedMetaCharacter = "\u2195";
            String openedMetaSuffix = isInsideNonEnabledMeta ? openedMetaCharacter : "";
            return "(" + (line + 1) + ":" + (offset + 1) + openedMetaSuffix + ")";
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
