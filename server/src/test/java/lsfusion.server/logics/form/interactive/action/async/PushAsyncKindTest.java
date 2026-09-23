package lsfusion.server.logics.form.interactive.action.async;

import org.junit.Test;

import static org.junit.Assert.*;

// #1841: a push is decoded only by a prediction of its own kind
public class PushAsyncKindTest {
    @Test
    public void closeReadsOnlyAClosePush() {
        AsyncCloseForm close = new AsyncCloseForm();
        assertTrue(close.deserializePush(new byte[] {2}) instanceof PushAsyncClose);
        // a value a custom view supplies (input kind) and an added object's id (add kind) are not a close confirmation
        assertNull(close.deserializePush(new byte[] {3, 0}));
        assertNull(close.deserializePush(new byte[] {4, 0, 0, 0, 0, 0, 0, 0, 7}));
    }
}
