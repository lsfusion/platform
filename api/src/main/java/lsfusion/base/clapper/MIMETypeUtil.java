package lsfusion.base.clapper;

import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The <tt>MIMETypeUtil</tt> class provides some general purpose MIME type
 * utilities not found in the JDK. Among other methods, this class provides the
 * {@link #fileExtensionForMIMEType fileExtensionForMIMEType()}
 * method, which converts a MIME type to a file extension. That method uses
 * a traditional <tt>mime.types</tt> files, similar to the file shipped
 * with with web servers such as Apache. It looks for a suitable file in
 * the following locations:
 *
 * <ol>
 * <li> First, it looks for the file <tt>.mime.types</tt> in the user's
 * home directory.
 * <li> Next, it looks for <tt>mime.types</tt> (no leading ".") in all the
 * directories in the CLASSPATH
 * <li> Last, it loads a default set of mappings shipped with this library
 * </ol>
 *
 * <p>It loads all the matching files it finds; the first mapping found for
 * a given MIME type is the one that is used. The files are only loaded once
 * within a given running Java VM.</p>
 *
 * <p>The syntax of the file follows the classic <tt>mime.types</tt>
 * syntax:</p>
 *
 * <pre>
 * # The format is &lt;mime type&gt; &lt;space separated file extensions&gt;
 * # Comments begin with a '#'
 *
 * text/plain             txt text TXT
 * text/html              html htm HTML HTM
 * ...
 * </pre>
 *
 * <p>When mapping a MIME type to an extension,
 * {@link #fileExtensionForMIMEType fileExtensionForMIMEType()}
 * uses the first extension it finds in the <tt>mime.types</tt> file.
 * MIME types that cannot be found in the file are mapped to extension
 * ".dat".</p>
 */
public class MIMETypeUtil {
    /*----------------------------------------------------------------------*\
                             Public Constants
    \*----------------------------------------------------------------------*/

    /**
     * Default MIME type, when a MIME type cannot be determined from a file's
     * extension.
     */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /*----------------------------------------------------------------------*\
                               Instance Data
    \*----------------------------------------------------------------------*/

    /**
     * Table for converting MIME type strings to file extensions. The table
     * is initialized the first time fileExtensionForMIMEType() is
     * called.
     */
    private static Map<String, String> mimeTypeToExtensionMap = null;

    /**
     * Reverse lookup table, by extension.
     */
    private static Map<String, String> extensionToMIMETypeMap = null;

    /*----------------------------------------------------------------------*\
                                Constructor
    \*----------------------------------------------------------------------*/

    private MIMETypeUtil() {
        // Can't be instantiated
    }

    /*----------------------------------------------------------------------*\
                              Public Methods
    \*----------------------------------------------------------------------*/

    /**
     * Get an appropriate extension for a MIME type.
     *
     * @param mimeType the String MIME type
     * @return the appropriate file name extension, or a default extension
     * if not found. The extension will not have the leading "."
     * character.
     */
    public static String fileExtensionForMIMEType(String mimeType) {
        loadMappings();

        String ext = mimeTypeToExtensionMap.get(mimeType);

        if (ext == null) ext = "dat";

        return ext;
    }

    /**
     * Get the MIME type for a filename extension.
     *
     * @param extension the extension, without the "."
     * @return the MIME type, or a default MIME type if there's no mapping
     * for the extension
     * @see #MIMETypeForFileExtension(String, String)
     * @see #MIMETypeForFileName(String, String)
     */
    public static String MIMETypeForFileExtension(String extension)  // NOPMD
    {
        return MIMETypeForFileExtension(extension, DEFAULT_MIME_TYPE);
    }

    /**
     * Get the MIME type for a filename extension.
     *
     * @param extension       the extension, without the "."
     * @param defaultMIMEType the default MIME type to use if one cannot
     *                        be determined from the extension, or null to
     *                        use {@link #DEFAULT_MIME_TYPE}
     * @return the MIME type, or the default MIME type
     * @see #MIMETypeForFileExtension(String)
     * @see #MIMETypeForFileName(String, String)
     */
    public static String MIMETypeForFileExtension(String extension,   // NOPMD
                                                  String defaultMIMEType) {
        return MIMETypeForFileName("test." + extension, defaultMIMEType);
    }

    /**
     * Get the MIME type for a file name. This method is simply a convenient
     * front-end for <tt>java.net.FileNameMap.getContentTypeFor()</tt>,
     * but it applies the supplied default when <tt>getContentTypeFor()</tt>
     * returns null (which can happen).
     *
     * @param fileName        the file name
     * @param defaultMIMEType the default MIME type to use if one cannot
     *                        be determined from the file name, or null to
     *                        use {@link #DEFAULT_MIME_TYPE}
     * @return the MIME type to use
     * @see #MIMETypeForFileExtension(String)
     * @see #MIMETypeForFileExtension(String, String)
     * @see #DEFAULT_MIME_TYPE
     */
    public static String MIMETypeForFileName(String fileName, String defaultMIMEType) {
        String mimeType;
        loadMappings();

        String extension = FileUtil.getFileNameExtension(fileName);
        mimeType = extensionToMIMETypeMap.get(extension);

        if (mimeType == null) { // Check the system one.
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
        }

        if (mimeType != null) {
            if (mimeType.equals(DEFAULT_MIME_TYPE) && (defaultMIMEType != null)) {
                // Substitute the caller's default, if there is one, on the
                // assumption that it'll be more useful.

                mimeType = defaultMIMEType;
            }
        } else {
            mimeType = (defaultMIMEType == null) ? DEFAULT_MIME_TYPE : defaultMIMEType;
        }

        return mimeType;
    }

    /*----------------------------------------------------------------------*\
                              Private Methods
    \*----------------------------------------------------------------------*/

    /**
     * Load the MIME type mappings into memory.
     */
    private static synchronized void loadMappings() {
        if (mimeTypeToExtensionMap != null) return;

        mimeTypeToExtensionMap = new HashMap<>();
        extensionToMIMETypeMap = new HashMap<>();

        ResourceBundle bundle = ResourceBundle.getBundle("MIMETypes");
        for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
            String type = (String) e.nextElement();

            String[] extensions = TextUtil.split(bundle.getString(type));

            if (mimeTypeToExtensionMap.get(type) == null) {
                mimeTypeToExtensionMap.put(type, extensions[0]);
            }

            for (int i = 0; i < extensions.length; i++) {
                if (extensionToMIMETypeMap.get(extensions[i]) == null) {
                    extensionToMIMETypeMap.put(extensions[i], type);
                }
            }

        }
    }

}
