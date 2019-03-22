package lsfusion.base.log;

public class StringDebugInfoWriter extends DebugInfoWriter {
    
    public StringBuilder stringBuilder = new StringBuilder();

    @Override
    public void addLines(String string) {
        if(stringBuilder.length() > 0)
            stringBuilder.append('\n');
        stringBuilder.append(string);
    }

    @Override
    protected StringDebugInfoWriter getStringDebugInfoWriter() {
        return this;
    }

    @Override
    protected String getTabPrefix() {
        return "";
    }
    
    // здесь != null явно, так как одно использование
    public String getString() {
        return stringBuilder.toString();
    }

}
