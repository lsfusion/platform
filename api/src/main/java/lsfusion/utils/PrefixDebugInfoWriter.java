package lsfusion.utils;

import lsfusion.base.BaseUtils;

public class PrefixDebugInfoWriter extends DebugInfoWriter {

    private final String tabPrefix;
    private final StringDebugInfoWriter debugInfoWriter;
    
    public PrefixDebugInfoWriter(String tabPrefix, StringDebugInfoWriter debugInfoWriter) {
        this.tabPrefix = tabPrefix;
        this.debugInfoWriter = debugInfoWriter;
    }

    @Override
    protected StringDebugInfoWriter getStringDebugInfoWriter() {
        return debugInfoWriter;
    }

    @Override
    protected String getTabPrefix() {
        return tabPrefix;
    }

    @Override
    public void addLines(String string) {
        debugInfoWriter.addLines(tabPrefix + BaseUtils.tabPrefix(string, tabPrefix));
    }
}
