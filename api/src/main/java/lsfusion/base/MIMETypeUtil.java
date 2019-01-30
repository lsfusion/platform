package lsfusion.base;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String extension = mimeTypeToExtensionMap.get(mimeType);
        if(extension == null) {
            Pattern p = Pattern.compile("\\b(application)/(\\w*)\\b");
            Matcher m = p.matcher(mimeType);
            if(m.find()) {
                extension = m.group(2);
            }
        }
        return extension;
    }

    public static String MIMETypeForFileExtension(String extension) {
        return MIMETypeForFileName(extension, "application/" + extension);
    }

    public static String MIMETypeForFileName(String extension, String defaultMimeType) {
        loadMappings();
        String mimeType = extensionToMIMETypeMap.get(extension);
        return mimeType == null ? defaultMimeType : mimeType;
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
