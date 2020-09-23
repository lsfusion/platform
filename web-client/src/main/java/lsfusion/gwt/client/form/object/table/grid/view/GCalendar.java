package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.function.Predicate;

public class GCalendar extends GSimpleStateTableView {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            calendar = createCalendar(element, list, calendarDateType);
        }
        JavaScriptObject events = remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), filterIdProperties()));
        updateEvents(calendar, events);
    }

    protected Predicate<GPropertyDraw> filterIdProperties() {
        return p -> !p.baseType.isLogicalOrDateTime();
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject objects, String calendarDateType)/*-{
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: calendarDateType === 'date' ? 'dayGridMonth' : 'dayGridWeek',
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek,timeGridDay'
            },
            dayMaxEvents: 4,
            defaultTimedEventDuration: "00:01",
            eventChange: function (info) {
                controller.changeDateTimeProperty(calendarDateType, objects[info.event.extendedProps.index], info.event.start.getFullYear(),
                    info.event.start.getMonth() + 1, info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                    info.event.start.getUTCMinutes(), info.event.start.getUTCSeconds());
            }
        });
        calendar.render();
        return calendar;
    }-*/;

    protected native JavaScriptObject remapList(JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns)/*-{
        var events = [];
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
        for (var i = 0; i < objects.length; i++) {
            var event = {
                'title': getTitle(objects[i]),
                'start': getEventStart(objects[i]),
                'end': getEventEnd(objects[i]),
                'editable': !controller.isPropertyReadOnly(calendarDateType ,objects[i]),
                'index': i
            };
            events.push(event);
        }
        return events;

        function getTitle(object) {
            var title = '';
            for (var x in columns) {
                if (title !== '')
                    continue;
                title = x === 'name' ? object[x] : '';
            }
            if (title === '' && columns.length >= 2) {
                for (var k = 0; k <= 2; k++) {
                    title = title + ' ' + object[columns[k]];
                }
            }
            return title;
        }

        function getEventStart(object) {
            return object[calendarDateType + 'From'] == null ? object[calendarDateType] : object[calendarDateType + 'From'];
        }

        function getEventEnd(object) {
            return object[calendarDateType + 'To'] == null ? '' : object[calendarDateType + 'To'];
        }
    }-*/;

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject events)/*-{
        calendar.setOption('events', events);
    }-*/;
}
