htmx.defineExtension('target-top', {
    onEvent: function(name, evt) {
        if (name === "htmx:beforeRequest") {
            let p = evt.detail.pathInfo.finalRequestPath;
            window.location.href = p.startsWith('/') ? p : window.location.href + p;
            evt.preventDefault();
        }
    }
});