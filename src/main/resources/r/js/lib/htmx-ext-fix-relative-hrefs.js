/**
 * Extension to fix relative href attributes of swapped content to point
 * to the source page instead of the current one.
 */
htmx.defineExtension('fix-relative-hrefs', {
    onEvent: function(name, evt) {
        var isRelative = function(href) {
            return !/^https?:\/\//i.test(href);
        }
        
        if (name === 'htmx:afterSwap') {
            if (!isRelative(evt.detail.pathInfo.requestPath)) {
                var requestUrl = new URL(evt.detail.pathInfo.requestPath);
                evt.detail.target.querySelectorAll('[href]').forEach(function(a) {
                    var href = a.getAttribute('href');
                    if (isRelative(href)) {
                        if (href.startsWith('/')) {
                            a.href = requestUrl.protocol + "//" +
                                     requestUrl.host +
                                     href;
                        } else {
                            var path = (evt.detail.pathInfo.responsePath || requestUrl.pathname).split('?')[0];
                            a.href = requestUrl.protocol + "//" +
                                     requestUrl.host +
                                     path +
                                     (path.endsWith('/') ? '' : '/') +
                                     href;
                        }
                    }
                });
            }
        }
    }
});
