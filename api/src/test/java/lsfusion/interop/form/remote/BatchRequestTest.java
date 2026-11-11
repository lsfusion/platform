package lsfusion.interop.form.remote;

import lsfusion.interop.form.property.EventSource;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class BatchRequestTest {

    // the log of the remote call that carries a batch names each request it carries with its arguments, as it names a
    // remote method's - a serialized value by its size -, not with its kind only
    @Test
    public void testNamedWithItsArguments() {
        assertNamed(new BatchRequest.ExecuteEventAction("change", new int[]{12}, new byte[][]{{1, 2}}, new EventSource[]{EventSource.EDIT}, new byte[][]{null}),
                "BatchRequest.ExecuteEventAction[", "actionSID=change", "propertyIDs={12}", "fullKeys={byte[2]}", "eventSources={EDIT}", "pushAsyncResults={null}");
        assertNamed(new BatchRequest.SetPropertyOrders(5, Collections.singletonList(10), Collections.singletonList(new byte[3]), Collections.singletonList(true)),
                "BatchRequest.SetPropertyOrders[", "groupObjectID=5", "propertyList={10}", "columnKeyList={byte[3]}", "orderList={true}");
        assertNamed(new BatchRequest.Unsupported("GetAsyncValues"), "BatchRequest.Unsupported[", "name=GetAsyncValues");
    }

    private static void assertNamed(BatchRequest request, String... arguments) {
        String named = request.toString();
        for (String argument : arguments)
            assertTrue(named, named.contains(argument));
    }
}
