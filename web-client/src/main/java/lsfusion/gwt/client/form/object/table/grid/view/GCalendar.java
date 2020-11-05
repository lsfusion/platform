package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

import java.util.Objects;

public class GCalendar extends GSimpleStateTableView {

    private final String calendarDateType;
    private JavaScriptObject calendar;
    private final JavaScriptObject controller;
    private JsArray<JavaScriptObject> list;
    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
        this.controller = getController();
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        this.list = list;
        if (calendar == null) {
            //fullcalendar bug - https://github.com/fullcalendar/fullcalendar/issues/5863
            //to prevent this when calendar-element height less then ~350px
            element.getParentElement().getStyle().setProperty("overflow", "auto");
            element.getStyle().setProperty("minHeight", "400px");
            element.getStyle().setProperty("cursor", "default");

            calendar = createCalendar(element, controller, calendarDateType, getEventName(list, "From"));
        }
        setRecordElement(calendar, recordElement);
        updateEvents(calendar, list, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()), controller);
    }

    protected native void setRecordElement(JavaScriptObject calendar, Element recordElement)/*-{
        calendar.recordElement = recordElement;
    }-*/;

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

    protected native String getInitialDate()/*-{
        var objects = this.@GCalendar::list;
        var initialObjectIndex;
        var startEventFieldName = this.@GCalendar::getEventName(*)(objects, 'From');
        for (var i = 0; i < objects.length; i++) {
            if (this.@GCalendar::isCurrentKey(*)(objects[i])) {
                initialObjectIndex = i;
            }
        }
        return objects[initialObjectIndex == null ? objects.length - 1 : initialObjectIndex][startEventFieldName];
    }-*/;

    protected boolean isCurrentKey(JavaScriptObject object){
        return Objects.equals(getKey(object), getCurrentKey());
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject controller, String calendarDateType, String datePropertyName)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            initialDate: this.@GCalendar::getInitialDate(*)(),
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: calendarDateType === 'dateTime' ? 'dayGridMonth,dayGridWeek,timeGridDay' : 'dayGridMonth,dayGridWeek'
            },
            dayMaxEvents: true,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventOrder: 'start,index',
            eventChange: function (info) {
                changeProperty(info, 'start', this.objects);
                changeProperty(info, 'end', this.objects);
            },
            datesSet: function () {
                var isDateTimeFilter = datePropertyName.toLowerCase().includes('time');
                var filterLeftBorder = calendar.view.activeStart;
                var filterRightBorder = calendar.view.activeEnd;
                controller.setViewFilter(filterLeftBorder.getFullYear(), filterLeftBorder.getMonth() + 1, filterLeftBorder.getUTCDate(),
                    filterRightBorder.getFullYear(), filterRightBorder.getMonth() + 1, filterRightBorder.getUTCDate(), datePropertyName, isDateTimeFilter);
            },
            eventClick: function (info) {
                changeCurrentEvent(info.event);

                setTooltip(info.el).show();
            }
        });
        calendar.render();
        return calendar;

        function setTooltip(el) {
            if (calendar.recordElement != null) {
                return $wnd.tippy(el, {
                    content: calendar.recordElement,
                    trigger: 'manual',
                    interactive: true,
                    allowHTML: true
                });
            }
        }

        function changeCurrentEvent(currentEvent) {
            var oldEvent = calendar.currentEventIndex != null ? calendar.getEvents()[calendar.currentEventIndex] : null;
            controller.changeSimpleGroupObject(calendar.objects[currentEvent.extendedProps.index], false);
            if (oldEvent !== null)
                oldEvent.setProp('classNames', '');
            currentEvent.setProp('classNames', 'event-highlight');
            calendar.currentEventIndex = currentEvent.extendedProps.index;
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

    protected native JavaScriptObject updateEvents(JavaScriptObject calendar, JavaScriptObject objects, JsArray<JavaScriptObject> columns, JavaScriptObject controller)/*-{
        var events = [];
        var startEventFieldName = this.@GCalendar::getEventName(*)(objects, 'From');
        var endEventFieldName = this.@GCalendar::getEventName(*)(objects, 'To');
        for (var i = 0; i < objects.length; i++) {
            var isCurrentKey = this.@GCalendar::isCurrentKey(*)(objects[i]);
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][startEventFieldName],
                'end': endEventFieldName != null ? objects[i][endEventFieldName] : null,
                'editable': !controller.isPropertyReadOnly(startEventFieldName, objects[i]) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, objects[i])),
                'durationEditable': endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, objects[i]),
                'allDay': this.@GCalendar::calendarDateType === 'date',
                'index': i,
                'startFieldName': startEventFieldName,
                'endFieldName': endEventFieldName,
                'classNames': isCurrentKey ? 'event-highlight' : ''
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
    }-*/;

    protected native String getEventName(JavaScriptObject objects, String position)/*-{
        var calendarDateType = this.@GCalendar::calendarDateType;
        if (objects.length > 0) {
            if (objects[0][calendarDateType + ''] != null)
                return calendarDateType;
            else
                return calendarDateType + position;
        } else
            return null;
    }-*/;
}
