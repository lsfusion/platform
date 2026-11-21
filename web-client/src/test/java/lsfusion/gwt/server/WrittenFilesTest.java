package lsfusion.gwt.server;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.http.controller.file.DownloadFileRequestHandler;
import lsfusion.http.controller.file.UploadFileRequestHandler;
import lsfusion.interop.logics.ServerSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Proxy;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

// the web server reads back (serves for download, or takes as an uploaded value) only a file it has written itself, by the
// address it gave it, and only while its content is unchanged - not whatever lies in or is put into its folders
public class WrittenFilesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File tempDir; // the web server's temp folder

    @Before
    public void setUp() throws IOException {
        tempDir = folder.newFolder("work");
        FileUtils.setTempFolder(tempDir.getPath());
    }

    @Test
    public void servesAWrittenFile() throws IOException {
        Response response = download(saveLogo());
        assertEquals(200, response.status);
        assertTrue(response.body().startsWith("logo "));
    }

    @Test
    public void doesNotServeAFilePutIntoTheFolder() throws IOException {
        for (String path : new String[]{"page.txt", "static/page.txt"}) { // the folder of a static file without and with a folder per store
            File planted = new File(tempDir, path);
            planted.getParentFile().mkdirs();
            Files.write(planted.toPath(), "other".getBytes(StandardCharsets.UTF_8));
        }

        Response response = download("file/static/page.txt?dumb=0");
        assertEquals(404, response.status);
        assertEquals("no-store", response.headers.get("Cache-Control")); // a file may be saved under that URL later
    }

    @Test
    public void writesOverAFileFoundWhereItWrites() throws IOException { // put there before, or left by the previous start
        RawFileData logo = new RawFileData(("logo " + Math.random()).getBytes(StandardCharsets.UTF_8));
        for (String path : new String[]{"noauth/", "static/noauth/"}) { // the folder without and with a folder per store
            File planted = new File(tempDir, path + logo.getID() + ".png");
            planted.getParentFile().mkdirs();
            Files.write(planted.toPath(), "other".getBytes(StandardCharsets.UTF_8));
        }

        assertTrue(download(FileUtils.saveApplicationFile(new FileData(logo, "png"))).body().startsWith("logo "));
    }

    @Test
    public void writesAnAddressOnceWhenSavedTwiceAtOnce() throws Exception { // the second save takes the first one's file, read once it is written
        String address = "static/once/x" + System.nanoTime() + ".txt";
        CountDownLatch writing = new CountDownLatch(1), written = new CountDownLatch(1);
        Thread first = new Thread(() -> FileUtils.writeFile(address, tempDir.getPath(), false, false, address, out -> {
            writing.countDown();
            try {
                written.await();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            out.write("first".getBytes(StandardCharsets.UTF_8));
        }));
        first.start();
        writing.await();

        int[] writes = new int[1];
        Thread second = new Thread(() -> FileUtils.writeFile(address, tempDir.getPath(), false, false, address, out -> writes[0]++));
        second.start();
        second.join(200); // done by now if it writes the file itself, waiting for the first one's write otherwise
        written.countDown();
        first.join();
        second.join();

        assertEquals(0, writes[0]);
        assertEquals("first", download("file/" + address + "?dumb=0").body());
    }

    @Test
    public void keepsNoAddressWhoseWriteFailed() throws IOException { // so the next save writes it
        String address = "static/failed/x" + System.nanoTime() + ".txt";
        try {
            FileUtils.writeFile(address, tempDir.getPath(), false, false, address, out -> {
                throw new IOException("no space left");
            });
            fail("a failed write is passed over");
        } catch (RuntimeException e) {
            assertEquals(404, download("file/" + address + "?dumb=0").status);
        }

        FileUtils.writeFile(address, tempDir.getPath(), false, false, address, out -> out.write("saved".getBytes(StandardCharsets.UTF_8)));
        assertEquals("saved", download("file/" + address + "?dumb=0").body());
    }

    @Test
    public void servesAVersionedFileOnlyByTheIssuedUrl() throws IOException {
        RawFileData css = new RawFileData(("body {} /* " + Math.random() + " */").getBytes(StandardCharsets.UTF_8));
        String url = FileUtils.saveWebFile("css/style.css", css, settings(), true);
        assertTrue(url.endsWith("style.css?version=" + css.getID()));

        assertEquals(200, download(url).status);
        assertEquals(404, download(url.replace("style.css?", "style.html?")).status); // the same file under another name and type
        assertEquals(404, download(url.substring(0, url.indexOf('?')) + "/" + css.getID() + "?dumb=0").status); // the name it is stored under
    }

    @Test
    public void writesAnImageWhenFirstAskedFor() throws IOException { // and over what lies there then, left by the previous start, say
        String ID = "image" + System.nanoTime();
        for (String path : new String[]{"app/test/icons/x.png/", "static/app/test/icons/x.png/"}) { // the folder without and with a folder per store
            File planted = new File(tempDir, path + ID);
            planted.getParentFile().mkdirs();
            Files.write(planted.toPath(), "other".getBytes(StandardCharsets.UTF_8));
        }

        int[] writes = new int[1];
        String url = FileUtils.saveImageFile("icons/x.png", out -> {
            writes[0]++;
            out.write("image".getBytes(StandardCharsets.UTF_8));
        }, ID, settings(), false);
        assertEquals(0, writes[0]);

        for (int i = 0; i < 2; i++) {
            assertEquals("image", download(url).body());
            assertEquals(1, writes[0]);
        }
    }

    @Test
    public void writesAVersionUnderEachNameApart() throws IOException { // so a first write of one never cuts a read of the other
        String ID = "image" + System.nanoTime();
        String add = FileUtils.saveImageFile("icons/add.png", out -> out.write("add".getBytes(StandardCharsets.UTF_8)), ID, settings(), false);
        String gif = FileUtils.saveImageFile("icons/add.gif", out -> out.write("gif".getBytes(StandardCharsets.UTF_8)), ID, settings(), false);

        assertEquals("add", download(add).body());
        assertEquals("gif", download(gif).body());
        assertEquals("add", download(add).body());
    }

    @Test
    public void doesNotServeAWrittenFileChangedAfterwards() throws IOException {
        String url = saveLogo();
        File written = findFile(tempDir, ".png");
        Files.write(written.toPath(), "something else".getBytes(StandardCharsets.UTF_8));

        assertEquals(404, download(url).status);
    }

    @Test
    public void servesABigWrittenFileOnlyWhileUnchanged() throws IOException { // checked by an extra read, not in memory
        byte[] bytes = new byte[2 * 1024 * 1024];
        new java.util.Random().nextBytes(bytes);
        String url = FileUtils.saveApplicationFile(new FileData(new RawFileData(bytes), "bin"));
        Response response = download(url);
        assertEquals(200, response.status);
        assertArrayEquals(bytes, response.body.toByteArray());

        bytes[bytes.length - 1]++;
        Files.write(findFile(tempDir, ".bin").toPath(), bytes);
        assertEquals(404, download(url).status);
    }

    @Test
    public void doesNotServeAStaticFileAsATemporaryOne() throws IOException {
        String url = saveLogo();
        File written = findFile(tempDir, ".png");

        assertEquals(404, download(url.replace("file/static/", "file/temp/")).status);
        assertTrue(written.exists());
        assertEquals(200, download(url).status);
    }

    @Test
    public void keepsADisplayNameInsideItsFolder() throws IOException { // a name with folders in it, from the data, stays in the file's folder
        FileData data = new FileData(new RawFileData(("named " + Math.random()).getBytes(StandardCharsets.UTF_8)), "txt");
        FileUtils.saveFormFile(data, "plain", null); // the file's own folder is there then
        String url = FileUtils.saveFormFile(data, "../../named", null);

        assertFalse(new File(folder.getRoot(), "named.txt").exists());
        assertEquals(200, download(url).status);
    }

    @Test
    public void keepsAnUploadInTheUploadFolder() throws Exception {
        assertEquals(0, upload("SID1", "x/dummy").status);
        assertEquals(0, upload("SID1", "x/../../outside.txt").status);

        assertFalse(new File(folder.getRoot(), "outside.txt").exists());
        assertTrue(new File(FileUtils.APP_UPLOAD_FOLDER_PATH, "outside.txt").exists());
    }

    @Test
    public void keepsAnUploadWithAFolderInItsSidInTheUploadFolder() throws Exception {
        assertEquals(0, upload("x/a", "page.txt").status);

        assertFalse(new File(FileUtils.APP_UPLOAD_FOLDER_PATH, "x").exists());
        assertTrue(new File(FileUtils.APP_UPLOAD_FOLDER_PATH, "a_page.txt").exists());
    }

    @Test
    public void keepsUploadsApartFromTheFilesToDownload() throws Exception { // an upload named as a file to download does not take its place
        RawFileData data = new RawFileData(("form file " + Math.random()).getBytes(StandardCharsets.UTF_8));
        String url = FileUtils.saveFormFile(new FileData(data, "txt"), null, null);
        assertEquals(0, upload("SID5", "/" + data.getID() + ".txt").status);

        Response response = download(url);
        assertEquals(200, response.status);
        assertTrue(response.body().startsWith("form file "));
    }

    @Test
    public void readsBackAnUploadByTheNameItWasGiven() throws Exception { // though it is kept under the last part of that name
        assertEquals(0, upload("SID4", "x/data.txt").status);
        Object value = FileUtils.readUploadFileAndDelete(new GFilesDTO("SID4_x/data.txt", "data.txt", false, false, false));
        assertEquals("hello", new String(((RawFileData) value).getBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void readsBackOnlyAnUploadedFile() throws Exception {
        File other = folder.newFile("other.txt");
        Files.write(other.toPath(), "other".getBytes(StandardCharsets.UTF_8));
        try {
            FileUtils.readUploadFileAndDelete(new GFilesDTO("../other.txt", "other.txt", false, false, false));
            fail("a file that was not uploaded is read");
        } catch (IllegalStateException e) {
            assertTrue(other.exists());
        }

        assertEquals(0, upload("SID2", "data.txt").status);
        Object value = FileUtils.readUploadFileAndDelete(new GFilesDTO("SID2_data.txt", "data.txt", false, false, false));
        assertEquals("hello", new String(((RawFileData) value).getBytes(), StandardCharsets.UTF_8));
        assertFalse(new File(FileUtils.APP_UPLOAD_FOLDER_PATH, "SID2_data.txt").exists());
    }

    // other content in each test: the written files are kept by the address, which a static file takes from its content
    private static String saveLogo() {
        return FileUtils.saveApplicationFile(new FileData(new RawFileData(("logo " + Math.random()).getBytes(StandardCharsets.UTF_8)), "png"));
    }

    private static ServerSettings settings() {
        return new ServerSettings("test", null, null, null, null, null, null, false, 0, false, null, false, null, null, null, false);
    }

    private static File findFile(File folder, String suffix) throws IOException {
        return Files.walk(folder.toPath()).map(java.nio.file.Path::toFile).filter(f -> f.isFile() && f.getName().endsWith(suffix)).findFirst().orElseThrow(IllegalStateException::new);
    }

    // url as FileUtils returns it: file/<path>?<params>
    private static Response download(String url) throws IOException {
        int query = url.indexOf('?');
        String path = url.substring(0, query);
        assertTrue(path.startsWith(FileUtils.DOWNLOAD_HANDLER + "/"));
        Map<String, String> parameters = new HashMap<>();
        for (String parameter : url.substring(query + 1).split("&")) {
            int eq = parameter.indexOf('=');
            parameters.put(parameter.substring(0, eq), parameter.substring(eq + 1));
        }
        String pathInfo = URLDecoder.decode(path.substring(FileUtils.DOWNLOAD_HANDLER.length()), "UTF-8");

        Response response = new Response();
        new DownloadFileRequestHandler().handleRequest(request(pathInfo, parameters, null, null), response.proxy());
        return response;
    }

    private static Response upload(String sid, String fileName) throws Exception {
        String boundary = "B0UNDARY";
        byte[] body = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: text/plain\r\n\r\nhello\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sid", sid);

        Response response = new Response();
        new UploadFileRequestHandler().handleRequest(request(null, parameters, "multipart/form-data; boundary=" + boundary, body), response.proxy());
        return response;
    }

    private static HttpServletRequest request(String pathInfo, Map<String, String> parameters, String contentType, byte[] body) {
        return (HttpServletRequest) Proxy.newProxyInstance(WrittenFilesTest.class.getClassLoader(), new Class[]{HttpServletRequest.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getPathInfo": return pathInfo;
                case "getParameter": return parameters.get((String) args[0]);
                case "getMethod": return "POST";
                case "getContentType": return contentType;
                case "getHeader": return "Content-type".equalsIgnoreCase((String) args[0]) ? contentType : null;
                case "getContentLength": return body.length;
                case "getContentLengthLong": return (long) body.length;
                case "getCharacterEncoding": return "UTF-8";
                case "setCharacterEncoding": return null;
                case "getInputStream":
                    ByteArrayInputStream in = new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        public boolean isFinished() { return in.available() == 0; }
                        public boolean isReady() { return true; }
                        public void setReadListener(ReadListener readListener) {}
                        public int read() { return in.read(); }
                    };
                default: return null;
            }
        });
    }

    private static class Response {
        int status; // 0 = not set (200 for a download)
        final Map<String, String> headers = new HashMap<>();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();

        String body() {
            return new String(body.toByteArray(), StandardCharsets.UTF_8);
        }

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(WrittenFilesTest.class.getClassLoader(), new Class[]{HttpServletResponse.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setStatus":
                    case "sendError":
                        status = (Integer) args[0];
                        return null;
                    case "setHeader":
                        headers.put((String) args[0], (String) args[1]);
                        return null;
                    case "getWriter": return new PrintWriter(body);
                    case "getOutputStream":
                        if (status == 0)
                            status = 200;
                        return new ServletOutputStream() {
                            public boolean isReady() { return true; }
                            public void setWriteListener(WriteListener writeListener) {}
                            public void write(int b) { body.write(b); }
                        };
                    default: return null;
                }
            });
        }
    }
}
