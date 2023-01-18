htmx.defineExtension('target-top', {
    onEvent: function(name, evt) {
        if (name === "htmx:beforeRequest") {
            evt.preventDefault();
            window.location.assign(evt.detail.pathInfo.requestPath);
        }
    }
});