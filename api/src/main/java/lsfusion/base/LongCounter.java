package lsfusion.base;

public class LongCounter {
    private long value;

    public long getValue() {
        return value;
    }

    public void add(long addValue) {
        value += addValue;
    }
}
