
function calendar(element, listJsObject){

    var events = [];
    for (let i = 0; i < listJsObject.length; i++) {
        var parse = JSON.parse(JSON.stringify(listJsObject[i]));
        var obj = {
            'title': parse['hostname(l)'],
            'start': parse['time(l)']
        }
        events.push(obj);
    }

    if( document.readyState !== 'loading' ) {
        var htmlDivElement = document.createElement("div");
        htmlDivElement.id = 'calendar';
        htmlDivElement.style.position = 'absolute';
        htmlDivElement.style.top = '0px';
        htmlDivElement.style.left = '0px';
        htmlDivElement.style.bottom = '0px';
        htmlDivElement.style.right = '0px';

        element.appendChild(htmlDivElement);
        var calendar = new FullCalendar.Calendar(htmlDivElement, {
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            editable: true,
            dayMaxEvents: true, // when too many events in a day, show the popover
            events: events
        });
        calendar.render();
    }
}