FullCalendar.globalLocales.push(function () {
    'use strict';

    return {
        code: "be",
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4  // The week that contains Jan 4th is the first week of the year.
        },
        buttonText: {
            prev: "Папярэдні",
            next: "Наступны",
            today: "Сёння",
            month: "Месяц",
            week: "Тыдзень",
            day: "Дзень",
            list: "Парадак дня"
        },
        weekText: "Тыдз",
        allDayText: "Увесь дзень",
        moreLinkText: function (n) {
            return "+ яшчэ " + n;
        },
        noEventsText: "Няма падзей для адлюстравання"
    };
}());
