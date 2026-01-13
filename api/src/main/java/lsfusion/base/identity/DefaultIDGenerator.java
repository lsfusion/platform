package lsfusion.base.identity;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultIDGenerator implements IDGenerator {

    AtomicInteger atomicInteger = new AtomicInteger(0);

    public synchronized int id() {
        return atomicInteger.getAndIncrement();
    }
}
