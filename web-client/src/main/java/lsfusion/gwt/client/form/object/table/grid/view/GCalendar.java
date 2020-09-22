package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GDateTimeType;
import lsfusion.gwt.client.classes.data.GDateType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.classes.data.GTimeType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.List;
import java.util.stream.Collectors;

public class GCalendar extends GSimpleStateTableView {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        JavaScriptObject events = remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), filterProperties(properties)));
        if (calendar == null) {
            calendar = createCalendar(element, list, calendarDateType, events);
        } else {
            updateEvents(calendar, events); //'setOption(calendar, optionName, optionValue)' ??
        }
    }

    protected List<GPropertyDraw> filterProperties(List<GPropertyDraw> properties) {
        return properties.stream()
                .filter(p -> !(p.baseType instanceof GLogicalType)
                        && !(p.baseType instanceof GDateTimeType)
                        && !(p.baseType instanceof GDateType)
                        && !(p.baseType instanceof GTimeType))
                .collect(Collectors.toList());
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject objects, String calendarDateType, JavaScriptObject events)/*-{
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
            editable: true,
            dayMaxEvents: 4,
            defaultTimedEventDuration: "00:01",
            events: events,
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
        for (var i = 0; i < objects.length; i++) {
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][calendarDateType + 'From'] == null ? objects[i][calendarDateType] : objects[i][calendarDateType + 'From'],
                'end': objects[i][calendarDateType + 'To'] == null ? '' : objects[i][calendarDateType + 'To'],
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
    }-*/;

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject events)/*-{
        calendar.setOption('events', events);
    }-*/;
}
