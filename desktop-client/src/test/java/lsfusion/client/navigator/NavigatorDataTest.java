package lsfusion.client.navigator;

import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;
import org.junit.Test;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigatorDataTest {
    // the web client builds data.windows from these values, so reading a navigator tree must keep the server's order
    @Test
    public void navigatorWindowsKeepTheirSerializedOrder() throws IOException {
        List<String> names = Arrays.asList("App.z", "App.a", "App.m");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(names.size());
        for (String name : names) {
            writeWindow(out, name);
            out.writeInt(SwingConstants.VERTICAL);
            out.writeBoolean(true);
            out.writeInt(SwingConstants.CENTER);
            out.writeInt(SwingConstants.TRAILING);
            out.writeInt(SwingConstants.CENTER);
            out.writeInt(SwingConstants.LEFT);
            out.writeFloat(0);
            out.writeFloat(0);
            out.writeBoolean(true);
        }
        out.writeInt(0); // no navigator elements
        out.writeInt(0); // no navigator changes
        writeWindow(out, "System.log");
        out.writeInt(1);
        writeWindow(out, "System.forms");
        SerializationUtil.writeString(out, "AppLayout");
        out.writeInt(0); // no schedulers

        NavigatorData data = NavigatorData.deserializeListClientNavigatorElementWithChildren(bytes.toByteArray());
        List<String> actual = new ArrayList<>();
        for (ClientNavigatorWindow window : data.windows.values())
            actual.add(window.canonicalName);
        assertEquals(names, actual);
    }

    private static void writeWindow(DataOutputStream out, String name) throws IOException {
        out.writeUTF(name);
        out.writeUTF(name);
        out.writeInt(WindowType.DOCKING_POSITION);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(100);
        out.writeInt(100);
        out.writeBoolean(true);
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.writeBoolean(false);
        SerializationUtil.writeString(out, null);
        out.writeBoolean(false);
        out.writeBoolean(false);
        out.writeInt(0);
        out.writeBoolean(false);
    }
}
