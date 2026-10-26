package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GColorType;
import lsfusion.gwt.client.classes.data.GFileType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.io.Serializable;
import java.util.ArrayList;

// A VALUE AS JS HAS IT - a view's or a renderer's function, a React view, a JS action, whoever draws the property -,
// and back: a value of a type given to JS (convertToJSValue), and the kind it is given as (getJSTypeName); what JS
// gives back read as a value of a type (convertFromJSValue), or of none, the server's type projecting it
// (convertFromUnknownJSValue); and the casts between Java's values and JS's own they are made of
public class JSValueConverter {

    public static PValue convertFromJSUndefValue(GPropertyDraw property, JavaScriptObject value) {
        return convertFromJSUndefValue(property.getExternalChangeType(), value);
    }
    public static PValue convertFromJSUndefValue(GType type, JavaScriptObject value) {
        if(GwtClientUtils.isUndefined(value))
            return PValue.UNDEFINED;

        return convertFromJSValue(type, value);
    }
    public static PValue convertFromJSValue(GType type, JavaScriptObject value) {
        if (type != null) // symmetric with convertToJSValue: an object's value is read back as its long id
            type = type.getDataType();
        // have to reverse convertToJSValue as well as convertFileValue (???)
        // NB: null/undefined MUST be tested on the raw JS value via JSNI (isUndefinedOrNull), never with a Java
        // `value == null`: a JS primitive 0 / false / "" carried in a JavaScriptObject reference reads as null under
        // GWT's `== null`, so a pushed 0 (or empty string, or 3-state false) would be silently dropped to NULL
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return PValue.getPValue(toBoolean(value));

            return PValue.getPValue(!GwtClientUtils.isUndefinedOrNull(value) ? toBoolean(value) : null);
        }
        if(GwtClientUtils.isUndefinedOrNull(value))
            return null;
        if(type instanceof GIntegralType)
            return ((GIntegralType) type).fromDoubleValue(toDouble(value));
        if(type instanceof GJSONType || (type == null && !(value instanceof Serializable))) // if type == null and incorrect value is passed, value will be not serializable and there will be an exception
            return PValue.getPValue(GwtClientUtils.jsonStringify(value));
        if (type instanceof GADateType)
            return ((GADateType) type).fromJsDate((JsDate)value);
        // a color and an interval go out as strings of their own (convertToJSValue: the value's toString), and come
        // back as what they are - a string the server is handed for one is no color and no interval to it
        if (type instanceof GColorType) {
            String color = GwtClientUtils.isJSString(value) ? toString(value) : null; // "#rrggbb" (ColorDTO)
            if (color == null || !color.matches("#[0-9a-fA-F]{6}"))
                throw GwtClientUtils.refusal(notTakenBack(value, "a color", "'#rrggbb'"));
            return PValue.getPValue(new ColorDTO(color.substring(1)));
        }
        if (type instanceof GIntervalType) {
            String interval = GwtClientUtils.isJSString(value) ? toString(value) : null; // "<from>.<to>" (GIntervalValue)
            int dot = interval != null ? interval.indexOf('.') : -1;
            try {
                if (dot > 0)
                    return PValue.getPValue(Long.parseLong(interval.substring(0, dot)), Long.parseLong(interval.substring(dot + 1)));
            } catch (NumberFormatException e) {
                // said below, as a string with no dot is
            }
            throw GwtClientUtils.refusal(notTakenBack(value, "an interval", "'<from>.<to>', its bounds in milliseconds"));
        }

        return PValue.getPValue(toString(value));
    }
    private static String notTakenBack(JavaScriptObject value, String what, String shape) {
        return GwtClientUtils.jsonStringify(value) + " is not " + what + ": " + what + " is given in the shape it is projected in, " + shape;
    }

    // JS-VALUE STRATEGY SEAM (V1 server-projection; V2 client-typed). Switch point.
    // Returns a PValue, symmetric with convertFromJSValue(type, ...): the form controller's exec/eval/change is
    // type-agnostic (no GType on the client), so for V1 the encoding is type-agnostic canonical
    // (number -> Double, boolean -> Boolean, string -> String, Date -> offset-ISO String yyyy-MM-dd'T'HH:mm:ssXXX,
    // object/array -> JSON String; null/undefined -> null) and the resolved server type projects it. A future V2
    // would swap this body for a type-aware convertFromJSValue(fetchedType, ...) with no change to the callers.
    public static PValue convertFromUnknownJSValue(JavaScriptObject value) {
        if (GwtClientUtils.isUndefinedOrNull(value))
            return null;
        if (isBoolean(value))
            return PValue.getPValue(toBoolean(value));
        if (isNumber(value))
            return PValue.getPValue(toDouble(value));
        if (isDate(value))
            return PValue.getPValue(formatJSDateOffsetISO(value));
        if (GwtClientUtils.isJSString(value))
            return PValue.getPValue(toString(value));
        return PValue.getPValue(GwtClientUtils.jsonStringify(value)); // object / array
    }
    // PValue -> canonical wire value via convertFileValueBack (DisplayString -> rawString, else unwrap), the same
    // step GClientWebAction.onJSFunctionExecuted uses; for these type-agnostic values it is a plain unwrap today,
    // but keeping it here means a future V2 / new convertFileValueBack semantics land in one place.
    public static Serializable encodeUnknownJSValue(JavaScriptObject value) {
        return PValue.convertFileValueBack(convertFromUnknownJSValue(value));
    }
    public static ArrayList<Serializable> encodeUnknownJSValues(JavaScriptObject array) {
        ArrayList<Serializable> result = new ArrayList<>();
        int length = GwtClientUtils.jsArrayLength(array);
        for (int i = 0; i < length; i++)
            result.add(encodeUnknownJSValue(GwtClientUtils.jsArrayGet(array, i)));
        return result;
    }
    private static native boolean isBoolean(JavaScriptObject v) /*-{ return typeof v === 'boolean'; }-*/;
    private static native boolean isNumber(JavaScriptObject v) /*-{ return typeof v === 'number'; }-*/;
    private static native boolean isDate(JavaScriptObject v) /*-{ return Object.prototype.toString.call(v) === '[object Date]'; }-*/;
    // local components + numeric offset (-getTimezoneOffset() so +03:00 stays +03:00; zero -> +00:00, not Z), seconds always
    private static native String formatJSDateOffsetISO(JavaScriptObject d) /*-{
        function p2(n) { return (n < 10 ? '0' : '') + n; }
        var off = -d.getTimezoneOffset();
        var aoff = Math.abs(off);
        return d.getFullYear() + '-' + p2(d.getMonth() + 1) + '-' + p2(d.getDate()) + 'T' +
               p2(d.getHours()) + ':' + p2(d.getMinutes()) + ':' + p2(d.getSeconds()) +
               (off >= 0 ? '+' : '-') + p2(Math.floor(aoff / 60)) + ':' + p2(aoff % 60);
    }-*/;

    public static JavaScriptObject convertToJSValue(GType type, GPropertyDraw property, boolean imageToHTML, PValue value) {
        if (type != null) // an object's value is its long id; getDataType projects it as that long, symmetric with the change-input type
            type = type.getDataType();
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return fromBoolean(PValue.getBooleanValue(value));

            if(value != null)
                return fromBoolean(PValue.get3SBooleanValue(value));
        }
        if(value == null)
            return null;
        if(type instanceof GIntegralType)
            return fromDouble(((GIntegralType) type).getDoubleValue(value));
        if(property != null && property.isPredefinedImage()) {
            AppBaseImage imageValue = PValue.getImageValue(value);
            if(imageToHTML)
                return fromString(imageValue.createImageHTML());

            return fromObject(imageValue);
        }
        // a link to the file: a cell of a RenderedClass comes as an image made of the file (FormChanges.getConvertData),
        // whose src it is, a call's result - an image's too - as the path the file is saved under
        if(type instanceof GFileType)
            return fromString(PValue.getValue(value) instanceof AppBaseImage ? PValue.getImageValue(value).getImageElementSrc(true) : GwtClientUtils.getAppDownloadURL(PValue.getStringValue(value)));
        if(type instanceof GJSONType)
            return GwtClientUtils.jsonParse(PValue.getCustomStringValue(value));
        if (type instanceof GADateType)
            return ((GADateType) type).toJsDate(value);

        return fromString(PValue.getCustomStringValue(value));
    }
    // the KIND the value above was converted BY - read off the SAME switch, so the name and the value cannot drift.
    // Not `typeof` of the result in every case: "date" is the platform's date-and-time family, and "json" says the
    // value was PARSED, so what it lands as is whatever the JSON held. Every remaining case converts to a string (a
    // text, an image src, a file url), which is what "string" says here.
    public static String getJSTypeName(GType type) {
        if (type == null)
            return null;
        type = type.getDataType();
        if (type instanceof GLogicalType)
            return "boolean";
        if (type instanceof GIntegralType)
            return "number";
        if (type instanceof GJSONType)
            return "json";
        if (type instanceof GADateType)
            return "date";
        return "string";
    }

    public static JavaScriptObject convertToJSValue(GPropertyDraw property, PValue value, RendererType rendererType, boolean imageToHTML) {
        return convertToJSValue(property.getRenderType(rendererType), property, imageToHTML, value);
    }

    // ===== the casts between Java's values and JS's own
    public static native JavaScriptObject fromString(String string) /*-{
        return string;
    }-*/;
    public static native String toString(JavaScriptObject string) /*-{
        return string;
    }-*/;
    private static native JavaScriptObject fromDouble(double d) /*-{
        return d;
    }-*/;
    public static native int toInt(JavaScriptObject d) /*-{
        return d;
    }-*/;
    private static native double toDouble(JavaScriptObject d) /*-{
        return d;
    }-*/;
    public static native JavaScriptObject fromBoolean(boolean b) /*-{
        return b;
    }-*/;
    private static native boolean toBoolean(JavaScriptObject b) /*-{
        return b;
    }-*/;
    public static native <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;
    public static native <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;
}
