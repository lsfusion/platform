package lsfusion.server.logics.debug;

import lsfusion.base.Pair;

public class DebugInfo {
    
    public static class DebugPoint {
        public final String moduleName;
        public final int line;
        public final int offset;
        public boolean isInsideNonEnabledMeta;
        
        public DebugPoint(String moduleName, int line, int offset, boolean isInsideNonEnabledMeta) {
            this.moduleName = moduleName;
            this.line = line;
            this.offset = offset;
            this.isInsideNonEnabledMeta = isInsideNonEnabledMeta;            
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

    protected DebugPoint point;
    
    private boolean needToCreateDelegate = true;
    private Integer debuggerLine = null;
    private Integer debuggerOffset = null;
    
    protected DebugInfo(DebugPoint debugPoint) {
        this.point = debugPoint;
    }
    
    protected DebugInfo(DebugPoint debugPoint, int debuggerLine, int debuggerOffset) {
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
}
