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
    }
    
    private final String openedMetaCharacter = "\u2195";
    
    public DebugPoint point;
    
    private boolean needToCreateDelegate = true;
    public Integer debugLine = null;
    public Integer debugOffset = null;
    
    public DebugInfo(DebugPoint debugPoint) {
        this.point = debugPoint;
    }
    
    public DebugInfo(DebugPoint debugPoint, int debugLine, int debugOffset) {
        this(debugPoint);
        this.debugLine = debugLine;
        this.debugOffset = debugOffset;
    }

    public Pair<String, Integer> getDebuggerModuleLine() {
        return new Pair<>(point.moduleName, getDebuggerLine());
    }

    public String getDebuggerMethodName(boolean firstInLine) {
        return "action_" + getDebuggerLine() + (firstInLine ? "" : "_" + getDebuggerOffset());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DebugInfo that = (DebugInfo) o;

        return point.line == that.point.line &&
                point.offset == that.point.offset &&
                point.moduleName.equals(that.point.moduleName);
    }

    @Override
    public int hashCode() {
        int result = point.moduleName.hashCode();
        result = 31 * result + point.line;
        result = 31 * result + point.offset;
        return result;
    }

    public int getDebuggerLine() {
        return debugLine == null ? point.line : debugLine;
    }
    
    public int getDebuggerOffset() {
        return debugOffset == null ? point.offset : debugOffset;
    }   
    
    @Override
    public String toString() {
        String openedMetaSuffix = point.isInsideNonEnabledMeta ? openedMetaCharacter : ""; 
        return point.moduleName + "(" + (point.line + 1) + ":" + (point.offset + 1) + openedMetaSuffix + ")";
    }

    public void setNeedToCreateDelegate(boolean needToCreateDelegate) {
        this.needToCreateDelegate = needToCreateDelegate;
    }

    public boolean needToCreateDelegate() {
        return needToCreateDelegate && !point.isInsideNonEnabledMeta;    
    } 
}
