package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.col.MapFact;
import lsfusion.interop.form.FormActivateType;
import lsfusion.interop.form.DockedWindowFormType;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.TopExecutionStack;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import org.junit.Test;

import static org.junit.Assert.*;

public class PushAsyncActivateTest {
    @Test
    public void absentPushOrPredictionHasNoResult() {
        assertNull(AsyncEventExec.deserializePush(null, () -> {
            fail("An absent push must not resolve its prediction");
            return null;
        }));
        assertNull(AsyncEventExec.deserializePush(new byte[] {1}, () -> null));
    }

    @Test
    public void typedConfirmationMatchesThePredictedAddress() {
        AsyncOpenForm prediction = new AsyncOpenForm("Test.form", null, null, FormActivateType.FIXED,
                null, false, new DockedWindowFormType("System.forms"));
        PushAsyncActivate push = (PushAsyncActivate) AsyncEventExec.deserializePush(new byte[] {1}, () -> prediction);
        assertFalse(push.matches("Test.other", null, "System.forms", FormActivateType.FIXED));
        assertFalse(push.matches("Test.form", "label", "System.forms", FormActivateType.FIXED));
        assertFalse(push.matches("Test.form", null, "Test.header", FormActivateType.FIXED));
        assertFalse(push.matches("Test.form", null, "System.forms", FormActivateType.USER));
        assertFalse(push.matches(null, null, "System.forms", FormActivateType.FIXED));
        assertFalse(push.matches("Test.form", null, "System.forms", null));
        assertTrue(push.matches("Test.form", null, "System.forms", FormActivateType.FIXED));
        assertTrue(push.matches("Test.form", null, "System.forms", FormActivateType.FIXED));
    }

    @Test
    public void closeAndInputCannotConfirmAnOpen() {
        AsyncOpenForm docked = new AsyncOpenForm("Test.form", null, null, FormActivateType.FIXED,
                null, false, new DockedWindowFormType("System.forms"));
        AsyncOpenForm floating = new AsyncOpenForm("Test.form", null, null, null,
                null, false, ModalityWindowFormType.FLOAT);
        AsyncCloseForm close = new AsyncCloseForm();

        assertNull(docked.deserializePush(new byte[] {2}));
        assertNull(floating.deserializePush(new byte[] {2}));
        assertNull(docked.deserializePush(new byte[] {3}));
        assertNull(close.deserializePush(new byte[] {1}));
        assertTrue(close.deserializePush(new byte[] {2}) instanceof PushAsyncClose);
    }

    @Test
    public void activationCannotConfirmAnOpenThatIsNotDocked() {
        AsyncOpenForm floating = new AsyncOpenForm("Test.form", null, null, FormActivateType.FIXED,
                null, false, ModalityWindowFormType.FLOAT);
        assertNull(floating.deserializePush(new byte[] {1}));
    }

    @Test
    public void matchingOpenConsumesPushAcrossSequentialChildren() {
        PushAsyncActivate push = new PushAsyncActivate("Test.form", "label", "Test.header", FormActivateType.USER);
        ExecutionContext<PropertyInterface> root = new ExecutionContext<>(MapFact.EMPTY(), push,
                null, null, null, null, new TopExecutionStack("test"), false);
        ExecutionContext<PropertyInterface> first = root.override();
        assertFalse(first.isPushedActivatedForm("Test.other", "label", "Test.header", FormActivateType.USER));
        assertTrue(first.isPushedActivatedForm("Test.form", "label", "Test.header", FormActivateType.USER));
        assertFalse(first.isPushedActivatedForm("Test.form", "label", "Test.header", FormActivateType.USER));
        assertFalse(root.override().isPushedActivatedForm("Test.form", "label", "Test.header", FormActivateType.USER));
    }
}
