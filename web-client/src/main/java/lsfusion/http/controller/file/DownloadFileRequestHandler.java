package lsfusion.http.controller.file;

import com.google.common.io.ByteStreams;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.controller.MainController;
import lsfusion.interop.session.ExternalUtils;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class DownloadFileRequestHandler implements HttpRequestHandler {

    public DownloadFileRequestHandler() {}

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo();

        boolean staticFile;
        String prefix;
        String storeSubdir;
        if(pathInfo.startsWith(prefix = "/" + FileUtils.STATIC_PATH + "/")) {
            staticFile = true;
            storeSubdir = FileUtils.STATIC_PATH;
        } else if(pathInfo.startsWith(prefix = "/" + FileUtils.TEMP_PATH + "/")) {
            staticFile = false;
            storeSubdir = FileUtils.TEMP_PATH;
        } else if(pathInfo.startsWith(prefix = "/" + FileUtils.DEV_PATH + "/")) {
            staticFile = true;
            storeSubdir = FileUtils.DEV_PATH;
        } else {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", MainController.getDirectUrl("/exec?action=getResource&p=" + pathInfo.replaceFirst("/", ""), request));
            return;
        }
        String fileName = pathInfo.substring(prefix.length());

        // MCP TEMP capability URLs go through `security="none"` — defense-in-depth: validate the
        // shape strictly before reading anything. Expected form: `mcp/<24 chars [0-9A-Z]>/<basename>`.
        // Restrict this check to TEMP paths so ordinary static/dev files under an `mcp/`
        // subdirectory keep the old /file behavior.
        boolean mcpTempFile = !staticFile && fileName.startsWith("mcp/");
        if (mcpTempFile && !isValidMCPPath(fileName)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid MCP file path");
            return;
        }

        String extension = BaseUtils.getFileExtension(fileName);

        String displayName = BaseUtils.getFileName(fileName);

        String version = request.getParameter("version");
        if (version != null) {
            if (mcpTempFile) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("MCP file URLs do not support version parameter");
                return;
            }
            if (!isValidVersion(version)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Illegal version parameter");
                return;
            }
            fileName = BaseUtils.replaceFileNameAndExtension(fileName, version);
        }

        // Defense-in-depth path-traversal check that applies to every branch (`temp`, `static`,
        // `dev`, `mcp`). Tomcat normalizes `..` segments in the URI by default, but we don't
        // rely on container-specific behavior — and `version` substring above only protects the
        // version parameter, not the rest of the file path. Resolve to canonical form and
        // require the result to stay inside `APP_DOWNLOAD_FOLDER_PATH/<storeSubdir>`. Catches
        // `../foo` in the URL path, weird absolute paths, and the post-version-replacement form
        // alike. The per-store subdir constraint also prevents store-confusion (a /file/temp/...
        // URL can only target physical files under the temp store).
        String storeRoot = FileUtils.APP_DOWNLOAD_FOLDER_PATH + "/" + storeSubdir;
        if (!isWithinFolder(storeRoot, fileName)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid file path");
            return;
        }

        Charset charset = ExternalUtils.downloadCharset;
        response.setContentType(ExternalUtils.getContentType(extension, charset).toString());
        //inline = open in browser, attachment = download
        response.addHeader(ExternalUtils.CONTENT_DISPOSITION_HEADER, ExternalUtils.getContentDisposition(displayName, extension, charset));
        // expiration will be set in urlRewrite.xml /file (just to have it at one place)

        // in theory e-tag and last modified may be send but since we're using "version" it's not that necessary

        // it seems that the browser might resend the request (for concurrent reading or whatever)
        FileUtils.readFile(storeRoot, fileName, !staticFile, true, inStream -> {
            ByteStreams.copy(inStream, response.getOutputStream());
        });
    }

    /**
     * Resolve {@code <root>/<relativePath>} to its canonical form (following symlinks,
     * collapsing {@code .} / {@code ..} segments) and confirm it stays <em>strictly inside</em>
     * the root — equality with the root itself is rejected too, since reading the directory
     * throws on {@code FileInputStream} and we don't want to surface that as a 500.
     * {@code IOException} during canonicalization (deep symlink loops, weird filesystem) is
     * treated as "reject" — same security stance, fail-closed.
     */
    private static boolean isWithinFolder(String root, String relativePath) {
        try {
            File rootDir = new File(root);
            String canonicalRoot = rootDir.getCanonicalPath();
            String canonicalRequested = new File(rootDir, relativePath).getCanonicalPath();
            return canonicalRequested.startsWith(canonicalRoot + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validate an MCP-issued file path: must be exactly {@code mcp/<24 chars [0-9A-Z]>/<basename>}
     * with no nested directories, no {@code ..} segments and no backslashes. The 24-char nonce
     * matches what {@link FileUtils#saveMCPFile} produces; anything else means a malformed
     * (or hostile) request and we refuse it before touching disk.
     */
    private static boolean isValidMCPPath(String path) {
        // Strip the "mcp/" prefix the caller already verified.
        String rest = path.substring("mcp/".length());
        int slash = rest.indexOf('/');
        if (slash != 24) return false;  // nonce must be exactly 24 chars and immediately followed by /
        for (int i = 0; i < 24; i++) {
            char c = rest.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z'))) return false;
        }
        String basename = rest.substring(slash + 1);
        if (basename.isEmpty()) return false;
        if (basename.indexOf('/') >= 0 || basename.indexOf('\\') >= 0) return false; // no nested directories
        // Reject only literal "." / ".." segments — the substring `..` is fine inside a single
        // filename and arises legitimately when adding an extension to a sanitized name like
        // "a." → "a..xlsx" (no path-traversal there, since the basename is one segment).
        if (basename.equals(".") || basename.equals("..")) return false;
        return true;
    }

    private static boolean isValidVersion(String version) {
        if (version.isEmpty()) return false;
        // Reject literal "." too — `replaceFileNameAndExtension(name, ".")` collapses the
        // resolved path to the parent directory, which would then 500 on `new FileInputStream`
        // (it's a directory, not a file). Better to fail fast with 400.
        if (version.equals(".")) return false;
        if (version.indexOf('/') >= 0 || version.indexOf('\\') >= 0) return false;
        if (version.contains("..")) return false;
        return true;
    }
}
