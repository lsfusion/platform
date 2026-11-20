package lsfusion.server.base;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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

    @Test
    public void aResourceIsClosedOnceItIsRead() {
        List<InputStream> streams = new ArrayList<>();
        ResourceUtils.calcFindResourceAs("system/Utils.lsf", stream -> {
            streams.add(stream);
            return stream.read();
        }, false, false, null, null);

        try {
            streams.get(0).read();
            fail("the resource is still open");
        } catch (IOException e) { // closed
        }
    }
}
