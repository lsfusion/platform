package lsfusion.server.logics.classes.data.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.language.ScriptedStringUtils;
import lsfusion.server.logics.classes.data.ByteArrayClass;
import lsfusion.server.physics.admin.files.JsonToolArgs;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


public abstract class FileClass<T> extends FileBasedClass<T> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(LocalizedString.create("{classes.file}"));

        this.multiple = multiple;
        this.storeName = storeName;
    }

    @Override
    public DBType getDBType() {
        return ByteArrayClass.instance;
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlBinary";
    }

    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }

    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBaseDotNetSize() {
        throw new UnsupportedOperationException();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getByteArraySQL();
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.CENTER;
    }

    protected abstract String getFileSID();

    @Override
    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
    }

    @Override
    public String getCanonicalName() {
        return getFileSID();
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    public String formatString(T value) {
        return value != null ? Base64.encodeBase64StringUnChunked(getRawFileData(value).getBytes()) : null;
    }

    @Override
    public String formatStringSource(String valueSource, SQLSyntax syntax) {
        return getEncode(getCastToStatic(valueSource));
    }

    public T parseString(String s) {
        return getValue(new RawFileData(Base64.decodeBase64(s)));
    }

    /**
     * Build an implicit body {@link ExternalRequest.Param} from one file-object entry.
     * The body param's url flag is false ⇒ implicit per
     * {@link ExternalRequest.Param#isImplicitParam()}, so it lands positionally in the
     * script's {@code run(...)} interfaces alongside string params from the same array.
     *
     * <p>Validation is fail-fast (mirrors what an MCP client expects from a strict tool):
     * <ul>
     *   <li>Exactly one of {@code base64} / {@code text} must be present.</li>
     *   <li>Malformed base64 surfaces as a clear {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * <p>Extension is resolved as: explicit {@code extension} field → extracted from
     * {@code fileName} (if it has a dot) → fallback {@code "file"}. {@code fileName} is
     * normalized through {@link BaseUtils#getFileName(String)}, so any directory prefix is
     * stripped before reaching the script.
     */
    public static ExternalRequest.Param buildFileParam(JSONObject fp, String origin) {
        for (String key : fp.keySet()) {
            if (!FILE_PARAM_KEYS.contains(key)) {
                throw new IllegalArgumentException(origin + " has unknown field `" + key
                        + "`; allowed: " + FILE_PARAM_KEYS);
            }
        }
        String b64 = JsonToolArgs.getStringAt(fp, "base64", origin + ".base64");
        String text = JsonToolArgs.getStringAt(fp, "text", origin + ".text");
        if (b64 != null && text != null) {
            throw new IllegalArgumentException(origin + " has both `base64` and `text`; pick one (mutually exclusive)");
        }
        byte[] bytes;
        if (b64 != null) {
            try {
                bytes = java.util.Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(origin + ".base64 is not valid base64: " + e.getMessage());
            }
        } else if (text != null) {
            bytes = text.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException(origin + " needs either `base64` or `text`");
        }

        // Resolve fileName + extension: explicit `extension` wins, else extracted from
        // `fileName` (if it has a dot), else fallback "file"; fileName is normalized to its
        // base name only (no directory part, no extension fragment).
        String rawFileName = nullIfEmpty(JsonToolArgs.getStringAt(fp, "fileName", origin + ".fileName"));
        String explicitExt = normalizeExtension(JsonToolArgs.getStringAt(fp, "extension", origin + ".extension"), origin);
        String extension = explicitExt;
        if (extension == null && rawFileName != null) {
            String fromName = BaseUtils.getFileExtension(rawFileName);
            if (!fromName.isEmpty()) extension = fromName;
        }
        if (extension == null) extension = "file";
        String fileName = rawFileName == null ? null : BaseUtils.getFileName(rawFileName);

        // url=false ⇒ implicit; fills the next free run() interface slot in array order.
        // Empty `name` keeps the param strictly positional — it can't accidentally match a
        // script-side interface name (lsFusion identifiers are never empty).
        FileData fileData = new FileData(new RawFileData(bytes), extension);
        return new ExternalRequest.Param(fileData, /*url=*/ false, StandardCharsets.UTF_8.name(), /*name=*/ "", fileName);
    }

    private static final Set<String> FILE_PARAM_KEYS = new LinkedHashSet<>(
            Arrays.asList("base64", "text", "fileName", "extension"));

    private static String nullIfEmpty(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    /**
     * Normalize a user-supplied {@code extension} field — descriptor calls for "without dot",
     * but agents commonly hand in {@code ".xlsx"} / {@code "  PDF  "} / etc. Strips
     * surrounding whitespace and any leading dots, then rejects path separators (a slash in
     * the extension is almost certainly a misuse: descriptor wanted just the suffix). An
     * empty result collapses to {@code null} so the fileName-derived fallback can take
     * over. Case is preserved on purpose — downstream {@code FileData.extension} feeds
     * lsFusion FORMAT lookups that may be case-sensitive in user code.
     */
    private static String normalizeExtension(String raw, String origin) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        int firstNonDot = 0;
        while (firstNonDot < trimmed.length() && trimmed.charAt(firstNonDot) == '.') firstNonDot++;
        trimmed = trimmed.substring(firstNonDot);
        if (trimmed.isEmpty()) return null;
        if (trimmed.indexOf('/') >= 0 || trimmed.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(origin + ".extension must not contain path separators; got `" + raw + "`");
        }
        return trimmed;
    }

    public abstract String getCastToStatic(String value);

    public String getCastToConvert(boolean needImage, String value, SQLSyntax syntax) {
        String stringConcatenate = syntax.getStringConcatenate();
        return "'" + ScriptedStringUtils.wrapFile((needImage ? "1" : "0") + serializeString(), "'" + stringConcatenate + getEncode(value) + stringConcatenate + "'") + "'";
    }

    protected abstract T readBytes(byte[] bytes);

    @Override
    public T read(Object value) {
        if(value instanceof byte[])
            return readBytes((byte[])value);
        return (T) value;
    }

    public T parseCast(String s) {
        return readBytes(Base64.decodeBase64(s));
    }

    private String getEncode(String valueSource) {
        // it's important to have it unchunked here, because when putting to json \n is escaped, and then in convertFileValue that \n goes to the base64 string and breaks everything
        return "replace(encode(" + valueSource + ", 'base64'), E'\\n', '')";
    }

    @Override
    public String formatUI(T object, String pattern) {
        if(object == null)
            return null;
        return getRawFileData(object).convertString();
    }

    protected abstract RawFileData getRawFileData(T value);
    protected abstract T getValue(RawFileData data);


    public String serializeString() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return IOUtils.serializeStream(outStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> FileClass<T> deserializeString(String image) throws IOException {
        DataInputStream dataStream = new DataInputStream(IOUtils.deserializeStream(image));
        return (FileClass) TypeSerializer.deserializeDataClass(dataStream);
    }
}
