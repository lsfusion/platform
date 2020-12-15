package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

public class GCalendar extends GTippySimpleStateTableView implements ColorThemeChangeListener {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;

        MainFrame.addColorThemeChangeListener(this);
    }

    @Override
    protected void render(Element element, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            //fullcalendar bug - https://github.com/fullcalendar/fullcalendar/issues/5863
            //to prevent this when calendar-element height less then ~350px
            element.getParentElement().getStyle().setProperty("overflow", "auto");
            element.getStyle().setProperty("minHeight", "400px");
            element.getStyle().setProperty("cursor", "default");
            String locale = LocaleInfo.getCurrentLocale().getLocaleName();

            calendar = createCalendar(element, controller, calendarDateType, locale);
        }
        updateEvents(calendar, list, calendarDateType, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()), controller);
    }

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject controller, String calendarDateType, String locale)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            timeZone: 'UTC',
            locale: locale,
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: calendarDateType === 'dateTime' ? 'dayGridMonth,dayGridWeek,timeGridDay' : 'dayGridMonth,dayGridWeek'
            },
            dayMaxEvents: true,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventDisplay: 'block', // no dots and correct work of backgroundColor/borderColor
            eventOrder: 'start,index',
            eventChange: function (info) {
                changeProperty(info, 'start', this.objects);
                changeProperty(info, 'end', this.objects);
            },
            datesSet: function (dateInfo) {
                var oldEvent = calendar.getEvents()[calendar.currentEventIndex];
                if (oldEvent != null && (oldEvent.start < dateInfo.start || oldEvent.start > dateInfo.end)) {
                    var event = getEvent(dateInfo, oldEvent.start <= dateInfo.start);
                    if (event !== null)
                        changeCurrentEvent(event, null);
                }
            },
            eventClick: function (info) {
                changeCurrentEvent(info.event, info.el);
            }
        });
        calendar.render();
        return calendar;

        function getEvent(dateInfo, getFirst) {
            var resultEvent = null;
            var events = calendar.getEvents();
            for (var i = 0; i < events.length; i++) {
                var event = events[i];
                if ((event.start >= dateInfo.start && event.start <= dateInfo.end) && (resultEvent == null || ((getFirst ? resultEvent.start > event.start : resultEvent.start < event.start))))
                    resultEvent = event;
            }
            return resultEvent;
        }

        function changeCurrentEvent(newEvent, elementClicked) {
            var newEventIndex = newEvent.extendedProps.index;
            var newObject = calendar.objects[newEventIndex];

            controller.changeSimpleGroupObject(newObject, true, elementClicked); // we're rerendering current event below

            if(elementClicked != null) {
                elementClicked[@lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder::COLUMN_ATTRIBUTE] = "true";
            }

            var oldEvent = calendar.currentEventIndex != null ? calendar.getEvents()[calendar.currentEventIndex] : null;
            if (oldEvent !== null) {
                oldEvent.setProp('classNames', '');
                oldEvent.setProp('backgroundColor', controller.getDisplayBackgroundColor(oldEvent.extendedProps.sourceBackgroundColor, false))
                oldEvent.setProp('borderColor', null);
            }
            newEvent.setProp('classNames', 'event-highlight');
            newEvent.setProp('backgroundColor', controller.getDisplayBackgroundColor(newEvent.extendedProps.sourceBackgroundColor, true))
            newEvent.setProp('borderColor', @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()());
            calendar.currentEventIndex = newEventIndex;
        }

        function changeProperty(info, position, objects) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            if (currentEvent[position] !== null && currentEvent[position].getTime() !== oldEvent[position].getTime()) {
                var propertyName = currentEvent.extendedProps[position + 'FieldName'];
                var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
                var eventElement = currentEvent[position];
                controller[controllerFunction](propertyName, objects[currentEvent.extendedProps.index], eventElement.getFullYear(),
                    eventElement.getMonth() + 1, eventElement.getUTCDate(), eventElement.getUTCHours(),
                    eventElement.getUTCMinutes(), eventElement.getUTCSeconds());
            }
        }
    }-*/;

    protected native JavaScriptObject updateEvents(JavaScriptObject calendar, JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns, JavaScriptObject controller)/*-{
        var events = [];
        calendar.currentEventIndex = null;
        for (var i = 0; i < objects.length; i++) {
            var object = objects[i]
            var startEventFieldName = getEventName(object, '') != null ? getEventName(object, '') : getEventName(object, 'From');
            var endEventFieldName = getEventName(object, 'To');
            var isCurrentKey = this.@GSimpleStateTableView::isCurrentObjectKey(*)(object);
            var backgroundColor = getBackgroundColor(object, controller, isCurrentKey);
            var foregroundColor = getForegroundColor(object, controller);
            var event = {
                'title': getTitle(object),
                'start': object[startEventFieldName],
                'end': endEventFieldName != null ? object[endEventFieldName] : null,
                'editable': !controller.isPropertyReadOnly(startEventFieldName, object) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, object)),
                'durationEditable': endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, object),
                'allDay': calendarDateType === 'date',
                'index': i,
                'startFieldName': startEventFieldName,
                'endFieldName': endEventFieldName,
                'classNames': isCurrentKey ? 'event-highlight' : '',
                'backgroundColor': backgroundColor.display,
                'sourceBackgroundColor': backgroundColor.source,
                'textColor': foregroundColor.display,
                'sourceTextColor': foregroundColor.source,
                'borderColor': isCurrentKey ? @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()() : null
            };
            events.push(event);
            if (isCurrentKey)
                calendar.currentEventIndex = i;
        }

        calendar.objects = objects;
        calendar.setOption('events', events);

        function getTitle(object) {
            var title = '';
            for (var i = 0; i < columns.length; i++) {
                if (title !== '')
                    continue;
                title = columns[i] === 'name' ? object[columns[i]] : '';
            }
            if (title === '' && columns.length >= 2) {
                for (var k = 0; k <= 2; k++) {
                    var value = object[columns[k]];
                    if (value != null)
                        title = title !== '' ? title + ' - ' + value : value;
                }
            }
            return title;
        }

        function getEventName(object, position) {
            if (object[calendarDateType + position] !== null && typeof object[calendarDateType + position] !== 'undefined') {
                return calendarDateType + position;
            } else {
                return null;
            }
        }
        
        function getBackgroundColor(object, controller, isCurrentKey) {
            var color = object.color
            if (!color) {
                color = controller.getGroupObjectBackgroundColor(object);
            }
            return {
                'source': color,
                'display': controller.getDisplayBackgroundColor(color, isCurrentKey) 
            };
        }
        
        function getForegroundColor(object, controller) {
            var color = controller.getGroupObjectForegroundColor(object);
            return {
                'source': color,
                'display': color ? @lsfusion.gwt.client.base.view.ColorUtils::getDisplayColor(Ljava/lang/String;)(color) : null
            };
        }
    }-*/;
    
    @Override
    public void colorThemeChanged() {
        updateEventsColors(calendar, controller);
    }

    protected native void updateEventsColors(JavaScriptObject calendar, JavaScriptObject controller)/*-{
        var events = calendar.getEvents();
        for (var i = 0; i < events.length; i++) {
            var event = events[i];
            var isCurrentKey = calendar.currentEventIndex === i;
            var displayBackgroundColor = controller.getDisplayBackgroundColor(event.extendedProps.sourceBackgroundColor, isCurrentKey);
            if (displayBackgroundColor) {
                event.setProp('backgroundColor', displayBackgroundColor)
            }
            if (event.extendedProps.sourceTextColor) {
                event.setProp('textColor', @lsfusion.gwt.client.base.view.ColorUtils::getDisplayColor(Ljava/lang/String;)(event.extendedProps.sourceTextColor))
            }
            if (isCurrentKey) {
                event.setProp('borderColor', @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()());
            }
        }
    }-*/;
}
