package lsfusion.server.physics.admin;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

// the defaults a server gets when nothing is set : the program interface is closed but for the actions marked for it.
// The field is read, not the getter, which answers 2 in development mode whatever the field is
public class SettingsDefaultsTest {

    @Test
    public void theApiIsClosedByDefault() throws ReflectiveOperationException {
        Field enableAPI = Settings.class.getDeclaredField("enableAPI");
        enableAPI.setAccessible(true);
        assertEquals((byte) 0, enableAPI.get(new Settings()));
    }
}
