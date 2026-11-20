package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

// unpackFile extracts into a fresh folder under java.io.tmpdir and deletes each file right after reading it, so an entry
// written outside would leave nothing new behind: every outside name points to a file created beforehand in
// java.io.tmpdir, which must keep its content
public class ZipUtilsTest {

    private final String id = UUID.randomUUID().toString();
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    private final List<File> kept = new ArrayList<>();
    private String absoluteName;

    @After
    public void tearDown() throws IOException {
        for (File file : kept)
            Files.deleteIfExists(file.toPath());
    }

    private File keptFile(File dir, String name) throws IOException {
        File file = new File(dir, name + "-" + id + ".txt");
        Files.write(file.toPath(), bytes("kept"));
        kept.add(0, file);
        return file;
    }

    // a file's path from the root with "/" and no drive letter: an entry name starting at the root, the same on any OS
    private static String absoluteName(File file) {
        return file.getAbsolutePath().replace('\\', '/').replaceFirst("^[A-Za-z]:", "");
    }

    // "a/ok.txt" comes first, so that the folder "a" exists when "a/../../" is unpacked; a name starting at the root is
    // unpacked under the extraction folder
    private Map<String, byte[]> entries() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a/ok.txt", bytes("inside"));
        entries.put("../" + keptFile(tmpDir, "up").getName(), bytes("outside"));
        entries.put("a/../../" + keptFile(tmpDir, "nested").getName(), bytes("outside"));
        entries.put("..\\" + keptFile(tmpDir, "backslash").getName(), bytes("outside"));
        absoluteName = absoluteName(keptFile(tmpDir, "absolute"));
        entries.put(absoluteName, bytes("inside"));
        entries.put("ok.txt", bytes("inside"));
        return entries;
    }

    private void check(Map<String, FileData> result) throws IOException {
        for (File file : kept)
            assertEquals(file + " changed", "kept", string(Files.readAllBytes(file.toPath())));
        assertEquals(new HashSet<>(Arrays.asList("a/ok.txt", absoluteName, "ok.txt")), result.keySet());
        for (FileData file : result.values())
            assertEquals("inside", string(file.getRawFile().getBytes()));
    }

    @Test
    public void zipEntriesStayInside() throws IOException {
        check(ZipUtils.unpackFile(new RawFileData(zip(entries(), null)), true));
    }

    @Test
    public void rar4EntriesStayInside() throws IOException {
        check(ZipUtils.unpackFile(new RawFileData(rar4(entries())), true));
    }

    @Test
    public void rar5EntriesStayInside() throws IOException {
        check(ZipUtils.unpackFile(new RawFileData(rar5(entries())), true));
    }

    // a symbolic link entry is unpacked as a plain file holding the link target, and nothing is written through it
    @Test
    public void zipSymlinkEntryIsNoLink() throws IOException {
        File target = new File(tmpDir, "link-target-" + id);
        assertTrue(target.mkdir());
        kept.add(target);
        File through = keptFile(target, "through");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("link", bytes(target.getAbsolutePath()));
        entries.put("link/" + through.getName(), bytes("inside"));
        Map<String, FileData> result = ZipUtils.unpackFile(new RawFileData(zip(entries, "link")), true);
        assertEquals("kept", string(Files.readAllBytes(through.toPath())));
        assertEquals(target.getAbsolutePath(), string(result.get("link").getRawFile().getBytes()));
        assertEquals("inside", string(result.get("link/" + through.getName()).getRawFile().getBytes()));
    }

    private static byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // archive builders

    private static byte[] zip(Map<String, byte[]> entries, String symlink) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        byte[] zip = bytes.toByteArray();
        if (symlink != null)
            assertTrue(markSymlink(zip, symlink));
        return zip;
    }

    // sets "made by Unix" and the S_IFLNK mode in the central directory record of the entry
    private static boolean markSymlink(byte[] zip, String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i + 46 < zip.length; i++)
            if (zip[i] == 'P' && zip[i + 1] == 'K' && zip[i + 2] == 1 && zip[i + 3] == 2
                    && (zip[i + 28] & 0xFF) == nameBytes.length && zip[i + 29] == 0
                    && Arrays.equals(Arrays.copyOfRange(zip, i + 46, i + 46 + nameBytes.length), nameBytes)) {
                zip[i + 5] = 3; // made by Unix
                int mode = 0120777;
                zip[i + 40] = (byte) (mode & 0xFF);
                zip[i + 41] = (byte) ((mode >> 8) & 0xFF);
                return true;
            }
        return false;
    }

    private static long crc(byte[] bytes, int from, int to) {
        CRC32 crc = new CRC32();
        crc.update(bytes, from, to - from);
        return crc.getValue();
    }

    private static void le(ByteArrayOutputStream out, long value, int size) {
        for (int i = 0; i < size; i++)
            out.write((int) (value >> (8 * i)) & 0xFF);
    }

    // RAR 4: stored (not compressed) files, the names as given
    private static byte[] rar4(Map<String, byte[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00}, 0, 7);
        rar4Block(out, 0x73, 0, new byte[6], null);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            byte[] data = entry.getValue();
            byte[] name = entry.getKey().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream fields = new ByteArrayOutputStream();
            le(fields, data.length, 4); // packed size
            le(fields, data.length, 4); // unpacked size
            fields.write(3); // host OS: Unix
            le(fields, crc(data, 0, data.length), 4);
            le(fields, 0x21010000L, 4); // time: 1996-08-01 00:00, DOS format
            fields.write(20); // version to extract
            fields.write(0x30); // method: store
            le(fields, name.length, 2);
            le(fields, 0100644, 4); // attributes
            fields.write(name, 0, name.length);
            rar4Block(out, 0x74, 0x8000, fields.toByteArray(), data);
        }
        rar4Block(out, 0x7B, 0x4000, new byte[0], null);
        return out.toByteArray();
    }

    private static void rar4Block(ByteArrayOutputStream out, int type, int flags, byte[] fields, byte[] data) {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        header.write(type);
        le(header, flags, 2);
        le(header, 7 + fields.length, 2);
        header.write(fields, 0, fields.length);
        byte[] bytes = header.toByteArray();
        le(out, crc(bytes, 0, bytes.length) & 0xFFFF, 2);
        out.write(bytes, 0, bytes.length);
        if (data != null)
            out.write(data, 0, data.length);
    }

    // RAR 5: stored files, the names as given
    private static byte[] rar5(Map<String, byte[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00}, 0, 8);
        ByteArrayOutputStream main = new ByteArrayOutputStream();
        vint(main, 1); // type: main archive header
        vint(main, 0); // header flags
        vint(main, 0); // archive flags
        rar5Block(out, main.toByteArray(), null);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            byte[] data = entry.getValue();
            byte[] name = entry.getKey().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream file = new ByteArrayOutputStream();
            vint(file, 2); // type: file header
            vint(file, 0x0002); // header flags: data area present
            vint(file, data.length); // data size
            vint(file, 0x0004); // file flags: data CRC32 present
            vint(file, data.length); // unpacked size
            vint(file, 0100644); // attributes
            le(file, crc(data, 0, data.length), 4);
            vint(file, 0); // compression: store
            vint(file, 1); // host OS: Unix
            vint(file, name.length);
            file.write(name, 0, name.length);
            rar5Block(out, file.toByteArray(), data);
        }
        ByteArrayOutputStream end = new ByteArrayOutputStream();
        vint(end, 5); // type: end of archive
        vint(end, 0);
        vint(end, 0);
        rar5Block(out, end.toByteArray(), null);
        return out.toByteArray();
    }

    private static void rar5Block(ByteArrayOutputStream out, byte[] header, byte[] data) {
        ByteArrayOutputStream sized = new ByteArrayOutputStream();
        vint(sized, header.length);
        sized.write(header, 0, header.length);
        byte[] bytes = sized.toByteArray();
        le(out, crc(bytes, 0, bytes.length), 4);
        out.write(bytes, 0, bytes.length);
        if (data != null)
            out.write(data, 0, data.length);
    }

    private static void vint(ByteArrayOutputStream out, long value) {
        do {
            int b = (int) (value & 0x7F);
            value >>>= 7;
            out.write(value != 0 ? b | 0x80 : b);
        } while (value != 0);
    }
}
