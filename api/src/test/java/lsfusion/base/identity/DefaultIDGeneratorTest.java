package lsfusion.base.identity;

import org.junit.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// the start numbers every action and property it makes from every parse thread at once (BaseLogicsModule.generateStaticNewID),
// and the generator's lock, around a counter atomic itself, was where the parse threads queued once the private group took none
public class DefaultIDGeneratorTest {

    @Test
    public void numbersWhileItsMonitorIsHeld() throws Exception {
        DefaultIDGenerator generator = new DefaultIDGenerator();
        generator.id();
        FutureTask<Integer> id = new FutureTask<>(generator::id);
        Thread thread = new Thread(id);
        synchronized (generator) { // as another thread inside a method synchronized on the generator
            thread.start();
            try {
                assertEquals(1, (int) id.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                fail("a thread taking a number waited for another one holding the generator");
            }
        }
        thread.join();
    }
}
