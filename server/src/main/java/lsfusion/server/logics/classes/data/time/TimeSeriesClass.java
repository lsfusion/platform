package lsfusion.server.logics.classes.data.time;

import lsfusion.base.DateConverter;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.type.DBType;
import lsfusion.server.logics.classes.data.FormatClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.function.Function;

public abstract class TimeSeriesClass<T extends Temporal> extends FormatClass<T> implements DBType {

    public TimeSeriesClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public DBType getDBType() {
        return this;
    }

    public abstract String getIntervalProperty();
    public abstract String getFromIntervalProperty();
    public abstract String getToIntervalProperty();

    public abstract String getDefaultPattern();

    public T parseIntervalUI(String value, String pattern) throws ParseException {
        return parseUI(value, pattern);
    }
    public T parseInterval(String value) throws ParseException {
        return parseString(value);
    }
    public String formatIntervalUI(T value, String pattern) {
        return formatUI(value, pattern);
    }
    public String formatInterval(T value) {
        return formatString(value);
    }

    // Shared parse pipeline (was duplicated per type): formatter parse -> ISO-offset projection -> smart-parse
    // fallback, all wrapped into a uniform ParseException. The three abstract hooks below isolate the only
    // per-type variation; the control flow, fallback order, and diagnostics are a base-class invariant, so this
    // is final.
    protected final T parseFormat(String s, DateTimeFormatter formatter) throws ParseException {
        try {
            try {
                return formatter.parse(s, getTemporalQuery());
            } catch (Exception ignored) {
            }
            // an ISO-8601 offset-bearing value (the form controller's date wire, see §13) -> this type, no tz
            // normalization; cheap, skips the smartParse regexps. Independent of isUseISOTimeFormatsInIntegration.
            T offset = parseISOOffset(s, getISOProjection());
            if(offset != null)
                return offset;
            return smartParse(s);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing " + getSID().toLowerCase(Locale.ROOT) + ": " + s, e);
        }
    }

    protected abstract TemporalQuery<T> getTemporalQuery();
    protected abstract Function<OffsetDateTime, T> getISOProjection();
    protected abstract T smartParse(String s);

    // Single place that parses the canonical offset-bearing ISO value and projects it per type; returns null
    // when the input is not an offset value, so parseFormat continues to its smart-parse fallback.
    protected static <R> R parseISOOffset(String value, Function<OffsetDateTime, R> projection) {
        OffsetDateTime offset = DateConverter.parseOffsetOrNull(value);
        return offset != null ? projection.apply(offset) : null;
    }

    // integration string parse/format formatter selection (isUseISOTimeFormatsInIntegration: ISO else locale),
    // centralized here; the lazy ternary picks WHICH hook to call, so the locale formatter (a ThreadLocal lookup)
    // is resolved only in locale mode. The per-type parseFormat (with its own smart-parse fallback) stays per type.
    protected abstract DateTimeFormatter getISOFormatter();
    protected abstract DateTimeFormatter getLocaleParser();
    protected abstract DateTimeFormatter getLocaleFormatter();

    @Override
    public T parseString(String s) throws ParseException {
        return parseFormat(s, Settings.get().isUseISOTimeFormatsInIntegration() ? getISOFormatter() : getLocaleParser());
    }

    @Override
    public String formatString(T value) {
        return value != null ? (Settings.get().isUseISOTimeFormatsInIntegration() ? getISOFormatter() : getLocaleFormatter()).format(value) : null;
    }

    @Override
    public T parseUI(String value, String pattern) throws ParseException {
        return parseFormat(value, getFormat(pattern));
    }

    @Override
    public String formatUI(T value, String pattern) {
        if(value == null)
            return null;

        return getFormat(pattern).format(value);
    }

    protected abstract String getIntervalString();

    public boolean hasInterval() {
        return getIntervalStepString() != null;
    }
    public String getIntervalStepString() {
        String intervalString = getIntervalString();
        if(intervalString == null)
            return null;

        return "interval '" + intervalString + "'";
    }

        @Override
    public String getIntervalStep() {
        return getIntervalStepString();
    }

    private DateTimeFormatter getFormat(String pattern) {
        return DateTimeFormatter.ofPattern(pattern != null ? pattern : getDefaultPattern());
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.pattern = getDefaultPattern();
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.END;
    }
}
