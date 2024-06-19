package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.AppStaticImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.cell.classes.*;

import java.io.Serializable;
import java.math.BigDecimal;

import static lsfusion.gwt.client.base.view.ColorUtils.convertToFontInfo;

public interface PValue {

    PValue UNDEFINED = new PValue() {};

    static PValue escapeSeparator(PValue value, GCompare compare) {
        if(compare != null) {
            Object ovalue = value;
            if (ovalue instanceof String) {
                return getPValue(GwtClientUtils.escapeSeparator(getStringValue(value), compare));
            } else if (ovalue instanceof DisplayString) {
                DisplayString dvalue = (DisplayString) ovalue;
                return new DisplayString(GwtClientUtils.escapeSeparator(dvalue.displayString, compare),
                                         GwtClientUtils.escapeSeparator(dvalue.rawString, compare));
            }
        }
        return value;
    }

    boolean useUnsafeCast = false;
    // ???? use unsafecast and : instead
    // Thank you Thomas, <set-property name="jre.checks.checkLevel" value="MINIMAL" /> in my gwt.xml file works :-)
    class SerializableValue implements PValue {

        public final Serializable value;

        public SerializableValue(Serializable value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof SerializableValue && value.equals(((SerializableValue) o).value);
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("getStringValue should be used instead");
//            return value.toString(); // pivoting uses toString, but mostly getStringValue should be used
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    static <T extends Serializable> T getValue(PValue value) { // private
        if(useUnsafeCast) {
            return (T) value;
        } else {
            if (value == null)
                return null;

            return (T) ((SerializableValue) value).value;
        }
//        return GwtClientUtils.unsafeCast(value);
    }
    static <T extends Serializable> PValue toPValue(T value) { // private
        if(useUnsafeCast) {
            return (PValue) value;
        } else {
            if (value == null)
                return null;

            return new SerializableValue(value);
        }
//        return GwtClientUtils.unsafeCast(value);
    }

    // temporary usage in pivot

    static Object getPivotValue(PValue value) {
        if(!useUnsafeCast) { // for pivoting we need actual numbers to sum them up (that's the idea of pivoting)
            // this way we can add "no toString" check (plus there might be some problems with ordering because of toString, but somewhy I haven't found them)
            if (value instanceof SerializableValue)
                return ((SerializableValue) value).value;
//            if (value instanceof SerializableValue && ((SerializableValue) value).value instanceof Number)
//                return getNumberValue(value);
        }
        return value;
    }
    static PValue getPivotPValue(Object value) {
        if(useUnsafeCast)
            return (PValue) value;
        else {
            if(value instanceof DisplayString) {
                return toPValue(((DisplayString) value).displayString);
            } else {
                // this way we can add "no toString" check
                return toPValue((Serializable) value);
            }
//            if(value instanceof Number) // for pivoting we need actual numbers to sum them up (that's the idea of pivoting)
//                return getPValue((Number)value);
//            return (PValue) value;
        }
    }

    static GDateTimeDTO getDateTimeValue(PValue value) {
        return getValue(value);
    }

    static GDateDTO getDateValue(PValue value) {
        return getValue(value);
    }

    static GTimeDTO getTimeValue(PValue value) {
        return getValue(value);
    }

    static GZDateTimeDTO getZDateTimeValue(PValue value) {
        return getValue(value);
    }

    static Number getNumberValue(PValue value) {
        return getValue(value);
    }

    static ColorDTO getColorValue(PValue value) {
        return getValue(value);
    }

    static AppBaseImage getImageValue(PValue value) {
        return getValue(value); // was converted in convertFileValue
    }

    static GFont getFontValue(PValue value) {
        return convertToFontInfo(getValue(value));
    }

    static String getColorStringValue(PValue value) {
        return getStringValue(value);
    }
    static String getClassStringValue(PValue value) { // css classes
        return getCustomStringValue(value);
    }
    static String getCustomStringValue(PValue value) { // everything html related
        return getStringValue(value);
    }
    static String getCaptionStringValue(PValue value) {
        return getStringValue(value);
    }

    static String getStringValue(PValue value) {
        if(value instanceof DisplayString)
            return ((DisplayString) value).displayString;
        Serializable data = getValue(value);
        return data != null ? data.toString() : null;
    }

    static Integer getIntegerValue(PValue value) {
        return getValue(value);
    }

    static BigDecimal getBigDecimalValue(PValue value) {
        return getValue(value);
    }

    static Long getIntervalValue(PValue o, boolean from) {
        assert o != null;
        String object = getBigDecimalValue(o).toString();
        int indexOfDecimal = object.indexOf(".");
        return Long.parseLong(indexOfDecimal < 0 ? object : from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1));
    }

    static Boolean get3SBooleanValue(PValue value) {
        return getValue(value);
    }

    static boolean getBooleanValue(PValue value) {
        return value != null;
    }

    static PValue getPValue(GDateTimeDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(GDateDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(GTimeDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(GZDateTimeDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(Number value) {
        return toPValue(value);
    }

    static PValue getPValue(ColorDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(GFilesDTO value) {
        return toPValue(value);
    }

    static PValue getPValue(String value) {
        return toPValue(value);
    }

    static PValue getPValue(BigDecimal value) {
        return toPValue(value);
    }

    static PValue getPValue(Long from, Long to) {
        return getPValue(new BigDecimal(from + "." + to));
    }

    static PValue getPValue(Boolean value) { // 3state boolean
        return toPValue(value);
    }

    static PValue getPValue(boolean value) {
        return toPValue(value ? true : null);
    }

    // client - server convertion

    class DisplayString implements PValue {
        public final String displayString;
        public final String rawString;

        public DisplayString(String displayString, String rawString) {
            this.displayString = displayString;
            this.rawString = rawString;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof DisplayString && displayString.equals(((DisplayString) o).displayString) && rawString.equals(((DisplayString) o).rawString);
        }

        @Override
        public String toString() {
            return displayString; // pivoting uses toString, but mostly getStringValue should be used
        }

        @Override
        public int hashCode() {
            return 31 * displayString.hashCode() + rawString.hashCode();
        }
    }

    static PValue convertFileValue(Serializable value) {
        return remapValue(value);
    }
    static PValue remapValue(Serializable value) {
        if (value instanceof GStringWithFiles) {
            GStringWithFiles stringWithFiles = (GStringWithFiles) value;
            StringBuilder result = new StringBuilder();
            for (int j = 0; j < stringWithFiles.prefixes.length; j++) {
                result.append(stringWithFiles.prefixes[j]);
                if(j < stringWithFiles.urls.length) {
                    Serializable url = stringWithFiles.urls[j];
                    if(url instanceof String) // file
                        result.append(GwtClientUtils.getAppStaticWebURL((String) url));
                    else {
                        AppStaticImage image = (AppStaticImage) url;
                        if(image != null)
                            result.append(image.createImageHTML());
                    }
                }
            }
            return new DisplayString(result.toString(), stringWithFiles.rawString);
        }
        return toPValue(value);
    }

    static Serializable remapValueBack(PValue value) {
        if(value instanceof DisplayString)
            return ((DisplayString) value).rawString;
        return getValue(value);
    }
}
