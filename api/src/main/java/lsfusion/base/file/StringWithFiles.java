package lsfusion.base.file;

import java.io.Serializable;

public class StringWithFiles implements Serializable {
    public String[] prefixes; //prefixes always exceed files by 1

    public Serializable[] files; // File or AppImage

    public static class File implements Serializable {
        public final String name;
        public final RawFileData raw;

        public File(RawFileData raw, String name) {
            this.name = name;
            this.raw = raw;
        }
    }

    public StringWithFiles(String[] prefixes, Serializable[] files) {
        this.prefixes = prefixes;
        this.files = files;
    }
}