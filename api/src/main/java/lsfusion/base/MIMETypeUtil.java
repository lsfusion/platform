package lsfusion.base;

import java.net.URLConnection;
import java.util.*;

/*
Copyright © 2004-2016, Brian M. Clapper. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the distribution.
Neither the names "clapper.org" nor the names of contributors may be used to endorse or promote products derived from this software
without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class MIMETypeUtil {

    private static Map<String, String> mimeTypeToExtensionMap = null;

    private static Map<String, String> extensionToMIMETypeMap = null;

    private MIMETypeUtil() {
    }

    public static String fileExtensionForMIMEType(String mimeType) {
        loadMappings();
        return mimeTypeToExtensionMap.get(mimeType);
    }

    public static String MIMETypeForFileExtension(String extension) {
        return MIMETypeForFileName("test." + extension, "application/" + extension);
    }

    public static String MIMETypeForFileName(String fileName, String defaultMimeType) {
        String mimeType;
        loadMappings();

        String extension = getFileNameExtension(fileName);
        mimeType = extensionToMIMETypeMap.get(extension);

        if (mimeType == null) { // Check the system one.
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
        }

        return mimeType == null ? defaultMimeType : mimeType;
    }

    private static String getFileNameExtension(String path) {
        String ext = null;
        int i = path.lastIndexOf('.');
        if ((i != -1) && (i != (path.length() - 1))) {
            ext = path.substring(i + 1);
        }
        return ext;
    }

    private static synchronized void loadMappings() {
        if (mimeTypeToExtensionMap != null) return;

        mimeTypeToExtensionMap = new HashMap<>();
        extensionToMIMETypeMap = new HashMap<>();

        ResourceBundle bundle = ResourceBundle.getBundle("MIMETypes");
        for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
            String type = (String) e.nextElement();

            String[] extensions = split(bundle.getString(type));

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

    private static String[] split(String s) {
        String[] result;
        StringTokenizer tok;
        Collection<String> temp = new ArrayList<>();
        tok = new StringTokenizer(s, " \t\n\r", false);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            temp.add(token);
        }
        result = new String[temp.size()];
        temp.toArray(result);
        return result;
    }
}
