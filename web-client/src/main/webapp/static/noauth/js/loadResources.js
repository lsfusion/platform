function loadResources(resources) {
    let alternativeName = null;
    let src;
    for (let i = 0; i < resources.length; i++) {
        if (typeof resources[i] === "object") {
            alternativeName = resources[i][1];
            src = resources[i][0];
        } else {
            alternativeName = null;
            src = resources[i];
        }
        document.write(getElement(src, alternativeName));
    }
}

function getElement(src, alternativeName) {
    // load local scripts due to "window.navigator.onLine" does not work as we need
    // let onlineStatus = window.navigator.onLine;
    let onlineStatus = false;

    if (!src.startsWith('http') || onlineStatus) {
        if (src.endsWith('.js')) {
            return `<script type=\"text/javascript\" src=\"${src}\"></script>`;
        } else {
            return `<link rel=\"stylesheet\" type=\"text/css\" href=\"${src}\" />`;
        }
    } else {
        src = alternativeName !== null ? alternativeName : src.split('/')[src.split('/').length - 1];
        if (src.endsWith('.js')) {
            return `<script type=\"text/javascript\" src=\"static/js/external/${src}\"></script>`;
        } else {
            return `<link rel=\"stylesheet\" type=\"text/css\" href=\"static/css/external/${src}\" />`;
        }
    }
}