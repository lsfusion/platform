package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static lsfusion.gwt.client.base.view.ColorUtils.getThemedColor;
import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.COLUMN_CLASS;

public class GCalendar extends GTippySimpleStateTableView implements ColorThemeChangeListener {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, TableContainer tableContainer, String calendarDateType) {
        super(form, grid, tableContainer);
        this.calendarDateType = calendarDateType;

        MainFrame.addColorThemeChangeListener(this);
    }

    @Override
    public int getDefaultPageSize() {
        return 10;
    }

    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            //fullcalendar bug - https://github.com/fullcalendar/fullcalendar/issues/5863
            //to prevent this when calendar-element height less then ~350px
//            element.getParentElement().getStyle().setProperty("overflow", "auto");
            element.getStyle().setProperty("minHeight", "400px");
            element.getStyle().setProperty("cursor", "default");
            String locale = LocaleInfo.getCurrentLocale().getLocaleName();

            calendar = createCalendar(element, controller, calendarDateType, locale);
        }
        updateEvents(list);
    }

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

    private static void mouseDown(Element element) {
        FocusUtils.focusOut(element, FocusUtils.Reason.MOUSENAVIGATE);
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject controller, String calendarDateType, String locale)/*-{
        var thisObj = this;
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            locale: locale,
            firstDay: 1,
            initialDate: controller.getValue(calendarDateType),
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: calendarDateType.includes('dateTime') ? 'dayGridMonth,dayGridWeek,timeGridDay' : 'dayGridMonth,dayGridWeek'
            },
            dayMaxEvents: true,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventDisplay: 'block', // no dots and correct work of backgroundColor/borderColor
            eventOrder: 'start,index',
            eventChange: function (info) {
                if (!@GCalendar::isUpdating) {
                    changeDateProperties(info);
                }
            },
            eventContent: function (arg) {
                return {
                    domNodes: [thisObj.@GCalendar::createImageCaptionElement(*)(arg.event.extendedProps.image, arg.event.extendedProps.caption, @lsfusion.gwt.client.base.ImageHtmlOrTextType::CALENDAR)]
                };
            },
            datesSet: function () {
                controller.setDateIntervalViewFilter(calendarDateType, @GCalendar::getEndEventFieldName(*)(calendarDateType), 1000,
                    calendar.view.activeStart, calendar.view.activeEnd, calendarDateType.toLowerCase().includes('time'));
            },
            eventClick: function (info) {
                changeCurrentEvent(info.event, info.el);
            }
        });
        calendar.render();

        // the problem is that in onPointerDown there is heuristics:
        // prevent links from being visited if there's an eventual drag.
        // also prevents selection in older browsers (maybe?).
        // not necessary for touch, besides, browser would complain about passiveness.
//        if (!ev.isTouch) {
//            ev.origEvent.preventDefault()
//        }
        // and this preventDefault prevents focus change, which leads to the problems with the popups for example (no focus out)
        // so we'll just emulate default behaviour
        element.addEventListener('mousedown', function(e) {
            @GCalendar::mouseDown(*)(e.target);
        });

        return calendar;

        function changeCurrentEvent(newEvent, elementClicked) {
            controller.changeObject(newEvent.extendedProps.object, true, elementClicked); // we're rerendering current event below
            @GCalendar::highlightEvent(*)(calendar, newEvent.extendedProps.key);
        }

        function changeDateProperties(info) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            var currentEventStart = currentEvent.start;
            var currentEventEnd = currentEvent.end;

            if ((getTime(currentEventStart) !== getTime(oldEvent.start)) || (getTime(currentEventEnd) !== getTime(oldEvent.end))) {
                var startFieldName = currentEvent.extendedProps.startFieldName;
                var endFieldName = currentEvent.extendedProps.endFieldName;

                if(currentEventEnd != null && currentEvent.allDay)
                    currentEventEnd.setDate(currentEventEnd.getDate() - 1);

                var object = info.event.extendedProps.object;
                controller.changeProperties(currentEventEnd != null ? [startFieldName, endFieldName] : [startFieldName], currentEventEnd != null ? [object, object] : [object],
                    currentEventEnd != null ? [currentEventStart, currentEventEnd] : [currentEventStart]);
            }
        }

        function getTime(eventTime) {
            //eventTime can be null due to property dateTo(dateTimeTo) can be null
            return eventTime != null ? eventTime.getTime() : null;
        }
    }-*/;

    private static String getEndEventFieldName(String calendarDateType) {
        return calendarDateType.contains("From") ? calendarDateType.replace("From", "To") : null;
    }

    private final NativeHashMap<GGroupObjectValue, Event> events = new NativeHashMap<>();
    private class Event {

        public final String title;
        public final String caption;
        public final BaseImage image;
        public final JavaScriptObject start;
        public final JavaScriptObject end;
        public final boolean editable;
        public final boolean durationEditable;
        public final boolean allDay;
        public final String startFieldName;
        public final String endFieldName;
        public final int index;
        public final GGroupObjectValue key;
        public final JavaScriptObject object;
        public final String backgroundColor;
        public final String foregroundColor;

        public Event(JavaScriptObject object, int index) {
            String endEventFieldName = getEndEventFieldName(calendarDateType);

            title = getTitle(object);
            caption = getCaption(object, GCalendar.this::getTitle);
            image = getImage(object, () -> null);
            start = getStart(object, calendarDateType);
            allDay = calendarDateType.equals("date") || calendarDateType.equals("dateFrom");
            end = endEventFieldName != null ? getEnd(object, endEventFieldName, allDay): null;
            editable = isEditable(object, controller, calendarDateType, endEventFieldName);
            durationEditable = isDurationEditable(object, controller, endEventFieldName);
            startFieldName = calendarDateType;
            endFieldName = endEventFieldName;
            this.index = index;
            this.key = getObjects(object);
            this.object = object;
            backgroundColor = getBackgroundColor(object, controller);
            foregroundColor = getForegroundColor(object, controller);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean isUpdating = false;

    private void updateEvents(JsArray<JavaScriptObject> list) {
        NativeHashMap<GGroupObjectValue, Event> oldEvents = new NativeHashMap<>();
        oldEvents.putAll(events);

        int fullUpdateLimit = 10;
        List<Runnable> eventsToAdd = new ArrayList<>();
        NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate = new NativeHashMap<>();

        for (int i = 0; i < list.length(); i++) {
            if (eventsToAdd.size() + eventsToUpdate.size() > fullUpdateLimit)
                break;

            JavaScriptObject object = list.get(i);
            GGroupObjectValue key = getObjects(object);

            Event event = new Event(object, i);
            events.put(key, event);

            Event oldEvent = oldEvents.remove(key);
            if (oldEvent == null)
                eventsToAdd.add(() -> addSingleCalendarEvent(calendar, createJsEvent(event, false)));
            else
                updateActionMap(eventsToUpdate, key, event, oldEvent);
        }
        oldEvents.foreachEntry((key, event) -> {
            events.remove(key);
            eventsToUpdate.put(key, this::removeSingleCalendarEvent);
        });

        GGroupObjectValue currentKey = getSelectedKey();

        if (eventsToAdd.size() + eventsToUpdate.size() > fullUpdateLimit) {
            events.clear();
            for (int i = 0; i < list.length(); i++) {
                JavaScriptObject object = list.get(i);
                events.put(getObjects(object), new Event(object, i));
            }
            setCalendarEvents(calendar, createCalendarEventsObject(events));
        } else {
            isUpdating = true;
            eventsToAdd.forEach(Runnable::run);
            processUpdateEvents(eventsToUpdate);
            highlightEvent(calendar, currentKey);
            isUpdating = false;
        }

        setCalendarCurrentEventId(calendar, currentKey);
    }

    private void processUpdateEvents(NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate) {
        JsArray<JavaScriptObject> calendarEvents = getCalendarEvents(calendar);
        for (int i = 0; i < calendarEvents.length(); i++) {
            JavaScriptObject eventToUpdate = calendarEvents.get(i);
            Consumer<JavaScriptObject> updateAction = eventsToUpdate.remove(getCalendarEventKey(eventToUpdate));
            if (updateAction != null)
                updateAction.accept(eventToUpdate);
        }
    }

    private void updateActionMap(NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate, GGroupObjectValue key, Event event, Event oldEvent) {
        Consumer<JavaScriptObject> updateAction = null;

        if (oldEvent.title == null || !oldEvent.title.equals(event.title))
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("title", event.title, calendarEvent), null);

        if (oldEvent.caption == null || !oldEvent.caption.equals(event.caption))
            updateAction = addUpdateAction(calendarEvent -> updateCalendarExtendedProperty("caption", event.caption, calendarEvent), updateAction);

        if (oldEvent.start != null && !GwtClientUtils.jsDateEquals(oldEvent.start, event.start))
            updateAction = addUpdateAction(calendarEvent -> updateStart(event.start, calendarEvent), updateAction);

        if (oldEvent.end != null && !oldEvent.end.equals(event.end))
            updateAction = addUpdateAction(calendarEvent -> updateEnd(event.end, calendarEvent), updateAction);

        if (oldEvent.editable != event.editable)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("editable", event.editable, calendarEvent), updateAction);

        if (oldEvent.durationEditable != event.durationEditable)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("durationEditable", event.durationEditable, calendarEvent), updateAction);

        if (oldEvent.index != event.index)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarExtendedProperty("index", event.index, calendarEvent), updateAction);

        if (!GwtClientUtils.nullEquals(oldEvent.backgroundColor, event.backgroundColor))
            updateAction = addUpdateAction(calendarEvent -> {
                updateCalendarProperty("backgroundColor", getThemedColor(event.backgroundColor), calendarEvent);
                updateCalendarExtendedProperty("sourceBackgroundColor", event.backgroundColor, calendarEvent);
            }, updateAction);

        if (!GwtClientUtils.nullEquals(oldEvent.foregroundColor, event.foregroundColor))
            updateAction = addUpdateAction(calendarEvent -> updateCalendarExtendedProperty("foregroundColor", getThemedColor(event.foregroundColor), calendarEvent), updateAction);

        if (updateAction != null)
            eventsToUpdate.put(key, updateAction);
    }

    private Consumer<JavaScriptObject> addUpdateAction(Consumer<JavaScriptObject> action, Consumer<JavaScriptObject> updateAction) {
        return updateAction == null ? action : updateAction.andThen(action);
    }

    private JsArray<JavaScriptObject> createCalendarEventsObject(NativeHashMap<GGroupObjectValue, Event> events){
        JsArray<JavaScriptObject> calendarEvents = JavaScriptObject.createArray().cast();
        events.foreachValue(event -> calendarEvents.push(createJsEvent(event, isCurrentKey(getObjects(event.object)))));
        return calendarEvents;
    }

    protected native void setCalendarCurrentEventId(JavaScriptObject calendar, GGroupObjectValue currentEventId)/*-{
        calendar.currentEventId = currentEventId;
    }-*/;

    protected native JsArray<JavaScriptObject> getCalendarEvents(JavaScriptObject calendar)/*-{
        return calendar.getEvents();
    }-*/;

    protected native GGroupObjectValue getCalendarEventKey(JavaScriptObject event)/*-{
        return event.extendedProps.key;
    }-*/;

    protected native void removeSingleCalendarEvent(JavaScriptObject event)/*-{
        event.remove();
    }-*/;

    protected native void addSingleCalendarEvent(JavaScriptObject calendar, JavaScriptObject event)/*-{
        // Necessary to add at least one eventSource if there was none.
        // Otherwise, on the first adding of a single event it will not be linked to any eventSource and after updating (going to previous / next month) the events will be duplicated
        if (calendar.getEventSources().length === 0)
            calendar.addEventSource([]);

        calendar.addEvent(event, true); //true, assign event to the first event source
    }-*/;

    protected static native void highlightEvent(JavaScriptObject calendar, GGroupObjectValue id)/*-{
        @GCalendar::isUpdating = true;
        var calendarEvents = calendar.getEvents();

        for (var i = 0; i < calendarEvents.length; i++) {
            var calendarEvent = calendarEvents[i];
            var key = calendarEvent.extendedProps.key;
            if (calendar.currentEventId != null && @lsfusion.gwt.client.base.GwtClientUtils::nullEquals(*)(key, calendar.currentEventId)) {
                calendarEvent.setProp('classNames', @GCalendar::getClassNames(*)(false));
                calendarEvent.setProp('backgroundColor', @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(calendarEvent.extendedProps.sourceBackgroundColor))
            }

            if (@lsfusion.gwt.client.base.GwtClientUtils::nullEquals(*)(key, id)) {
                calendarEvent.setProp('classNames', @GCalendar::getClassNames(*)(true));
                calendarEvent.setProp('backgroundColor', @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(calendarEvent.extendedProps.sourceBackgroundColor))
            }
        }
        calendar.currentEventId = id;
        @GCalendar::isUpdating = false;
    }-*/;

    protected native void setCalendarEvents(JavaScriptObject calendar, JsArray<JavaScriptObject> events)/*-{
        // Fullcalendar documentation describes that it is necessary to work eventSources(full update of events, such as deletion of all old events and addition of new ones)
        // and setting eventSources in setOption-method is not allowed!
        // https://fullcalendar.io/docs/dynamic-options

        var eventSources = calendar.getEventSources();
        for (var i = 0; i < eventSources.length; i++ ) {
            eventSources[i].remove();
        }
        calendar.addEventSource( events );
    }-*/;

    protected native void updateCalendarProperty(String propertyName, Object property, JavaScriptObject event)/*-{
        event.setProp(propertyName, property);
    }-*/;

    protected native void updateCalendarExtendedProperty(String propertyName, Object property, JavaScriptObject event)/*-{
        event.setExtendedProp(propertyName, property);
    }-*/;

    protected native void updateStart(JavaScriptObject start, JavaScriptObject event)/*-{
        event.setStart(start, {
            maintainDuration: true  //if set to true, the event’s end will also be adjusted in order to keep the same duration
        });
    }-*/;

    protected native void updateEnd(JavaScriptObject end, JavaScriptObject event)/*-{
        event.setEnd(end);
    }-*/;

    private JavaScriptObject createJsEvent(Event event, boolean isCurrentKey){
        return createEventAsJs(event.title, event.caption, event.image, event.start, event.end, event.editable, event.durationEditable,
                event.allDay, event.key, event.startFieldName, event.endFieldName, event.index, event.object, isCurrentKey, event.backgroundColor, event.foregroundColor);
    }

    protected Element getCellParent(Element target) {
        return GwtClientUtils.getParentWithClass(target, COLUMN_CLASS);
    }

    private static String getClassNames(boolean isCurrentKey) {
        return COLUMN_CLASS + (isCurrentKey ? " event-highlight" : "");
    }

    protected native JavaScriptObject createEventAsJs(String title, String caption, BaseImage image, JavaScriptObject start, JavaScriptObject end, boolean editable, boolean durationEditable,
                                                      boolean allDay, GGroupObjectValue key, String startFieldName, String endFieldName, int index, JavaScriptObject object,
                                                      boolean  isCurrentKey, String backgroundColor, String foregroundColor)/*-{
        return {
            title: title,
            caption: caption,
            image: image,
            start: start,
            end: end,
            editable: editable,
            durationEditable: durationEditable,
            allDay: allDay,
            startFieldName: startFieldName,
            endFieldName: endFieldName,
            index: index,
            key: key,
            object: object,
            sourceBackgroundColor: backgroundColor,
            sourceTextColor: foregroundColor,

            classNames: @GCalendar::getClassNames(*)(isCurrentKey),
            backgroundColor: @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(backgroundColor),
            textColor: @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(foregroundColor)
        };
    }-*/;

    private String getTitle(JavaScriptObject object) {
        String title = getTitle(object, getCaptions(new NativeHashMap<>(), (gPropertyDraw, columnName) -> gPropertyDraw.sticky || columnName.equals("name")));

        //if sticky columns less than two and there is no column with the name "name" in the list of columns then look for columns with "Id" valueType and use the first of them as a title.
        if (title != null && title.isEmpty()) {
            JsArray<JavaScriptObject> captions = getCaptions(new NativeHashMap<>(), (gPropertyDraw, columnName) -> gPropertyDraw.getValueType().isId());
            if (captions.length() > 0)
                title = String.valueOf(GwtClientUtils.getField(object, captions.get(0).toString()));
        }

        // to display null values as an empty string
        return title == null ? "" : title;
    }

    protected native String getTitle(JavaScriptObject object, JsArray<JavaScriptObject> columns)/*-{
        var title = columns.includes('name') ? object['name'] : '';

        if (title === '' && columns.length >= 2) {
            for (var k = 0; k <= 2; k++) {
                var value = object[columns[k]];
                if (value != null)
                    title = title !== '' ? title + ' - ' + value : value;
            }
        }
        return title;
    }-*/;

    protected native JavaScriptObject getStart(JavaScriptObject object, String startEventFieldName)/*-{
        return object[startEventFieldName];
    }-*/;

    protected native JavaScriptObject getEnd(JavaScriptObject object, String endEventFieldName, boolean allDay)/*-{
        var end = object[endEventFieldName];
        if (allDay)
            end.setDate(end.getDate() + 1); //adding time to Date causes that it will be impossible to change event on calendar-view even if "allDay" option is "true"

        return end;
    }-*/;

    protected native boolean isEditable(JavaScriptObject object, JavaScriptObject controller, String startEventFieldName, String endEventFieldName)/*-{
        return !controller.isPropertyReadOnly(startEventFieldName, object) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, object));
    }-*/;

    protected native boolean isDurationEditable(JavaScriptObject object, JavaScriptObject controller, String endEventFieldName)/*-{
        return endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, object);
    }-*/;

    protected native String getBackgroundColor(JavaScriptObject object, JavaScriptObject controller)/*-{
        return controller.getGroupObjectBackgroundColor(object);
    }-*/;

    protected native String getForegroundColor(JavaScriptObject object, JavaScriptObject controller)/*-{
        return controller.getGroupObjectForegroundColor(object);
    }-*/;

    @Override
    public void colorThemeChanged() {
        updateEventThemeColors(calendar);
    }

    protected native void updateEventThemeColors(JavaScriptObject calendar)/*-{
        var events = calendar.getEvents();
        for (var i = 0; i < events.length; i++) {
            var event = events[i];
            var displayBackgroundColor = @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(event.extendedProps.sourceBackgroundColor);
            if (displayBackgroundColor) {
                event.setProp('backgroundColor', displayBackgroundColor)
            }
            if (event.extendedProps.sourceTextColor) {
                event.setProp('textColor', @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(event.extendedProps.sourceTextColor))
            }
        }
    }-*/;

    @Override
    public boolean isDefaultBoxed() {
        return false;
    }
}
