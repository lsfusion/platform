package lsfusion.base.identity;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultIDGenerator implements IDGenerator {

    AtomicInteger atomicInteger = new AtomicInteger(0);

    // no lock : the start numbers every action and property it makes from every parse thread at once, and the counter is
    // atomic itself
    public int id() {
        return atomicInteger.getAndIncrement();
    }
}
