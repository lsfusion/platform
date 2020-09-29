function loadResources(resources) {
    let alternativeName = null;
    let src;
    for (let i = 0; i < resources.length; i++) {
        if (typeof resources[i] === "object") {
            alternativeName = resources[i][1];
            src = resources[i][0];
        } else {
            src = resources[i];
        }
        document.write(getElement(src, alternativeName));
    }
}

function getElement(src, alternativeName) {
    if (window.navigator.onLine) {
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
