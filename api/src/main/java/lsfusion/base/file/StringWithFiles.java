package lsfusion.base.file;

import java.io.Serializable;

public class StringWithFiles implements Serializable {
    public String[] prefixes; //prefixes always exceed files by 1

    public Serializable[] files; // File or AppImage

    public String rawString;

    // resource file - file + relative path in the resources
    public static class File implements Serializable {
        public final String name;
        public final RawFileData raw;

        public File(RawFileData raw, String name) {
            this.name = name;
            this.raw = raw;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof File && name.equals(((File) o).name) && raw.equals(((File) o).raw);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + raw.hashCode();
        }
    }

    public StringWithFiles(String[] prefixes, Serializable[] files, String rawString) {
        this.prefixes = prefixes;
        this.files = files;
        this.rawString = rawString;
    }
}