package lsfusion.server.base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

// a resource found by a name relative to the classpath : the server's own .lsf files are on the classpath of its tests
public class ResourceUtilsTest {

    @Test
    public void aNameWithAFolderFindsTheResourceInThatFolder() {
        assertEquals("/system/Utils.lsf", ResourceUtils.findResourcePath("system/Utils.lsf", true, true, null));
        assertNull(ResourceUtils.findResourcePath("utils/Utils.lsf", true, true, null));
    }

    @Test
    public void aNameWithoutAFolderFindsTheShallowestResource() {
        assertEquals("/system/Utils.lsf", ResourceUtils.findResourcePath("Utils.lsf", true, true, null));
    }

    @Test
    public void aNameIsMatchedAsItIsWritten() {
        assertNull(ResourceUtils.findResourcePath("syst(em/Utils.lsf", true, true, null));
        assertNull(ResourceUtils.findResourcePath("sys.em/Utils.lsf", true, true, null));
        assertNull(ResourceUtils.findResourcePath("system/Utils.[lsf", true, true, null));
    }
}
