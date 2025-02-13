package lsfusion.base.file;

import java.io.Serializable;

public class StringWithFiles implements Serializable {
    public String[] prefixes; //prefixes always exceed files by 1

    public Serializable[] files; // File or AppImage

    public String rawString;

    // resource file - file + relative path in the resources
    public static class Resource implements Serializable {
        public final String name;
        public final RawFileData raw;

        public Resource(RawFileData raw, String name) {
            this.name = name;
            this.raw = raw;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Resource && name.equals(((Resource) o).name) && raw.equals(((Resource) o).raw);
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

    @Override
    public String toString() {
        //string with files is supported only in web-client
        return rawString;
    }
}