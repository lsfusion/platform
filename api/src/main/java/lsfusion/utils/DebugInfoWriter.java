package lsfusion.utils;

import lsfusion.base.BaseUtils;

public abstract class DebugInfoWriter {

    public static DebugInfoWriter pushPrefix(DebugInfoWriter debugInfoWriter, String prefix) {
        return pushPrefix(debugInfoWriter, prefix, "");
    }
    public static DebugInfoWriter pushPrefix(DebugInfoWriter debugInfoWriter, String prefix, Object object) {
        if(debugInfoWriter != null)
            return debugInfoWriter.pushPrefix(prefix + " : " + object);
        return null;
    }

    public abstract void addLines(String string); // здесь != null явно чтобы toString и другие лишние обработки не вызывались

    public DebugInfoWriter pushPrefix(final String prefix) {
        addLines(prefix);
        return new PrefixDebugInfoWriter(getTabPrefix() + '\t', getStringDebugInfoWriter());
    }

    protected abstract StringDebugInfoWriter getStringDebugInfoWriter();;
    protected abstract String getTabPrefix();
}
