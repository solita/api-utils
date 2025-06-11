var initLayers = function(map, olstuff) {
    var params = new URLSearchParams(window.location.search);
    var layers = params.get('layers');
    if (layers) {
        var ls = layers.split(',');
        var highlighted = ls.filter(x => x.endsWith('!')).map(x => x.replace('!', ''));
        ls = ls.map(x => x.replace('!', ''));
        map.getLayers().getArray().map(olstuff.actualLayers).reduce(function(a,b) {return a.concat(b)}).forEach(function(l) {
            if (ls.includes(l.get('shortName'))) {
                if (highlighted.includes(l.get('shortName'))) {
                    l.set('_highlight', olstuff.styles.circle(10.0, 'rgba(255,0,0,0.2)'), true);
                }
                l.setVisible(true);
            }
        });
        map.getControls().getArray().filter(function(x) { return x.showPanel; }).forEach(function(x) { x.showPanel(); x.hidePanel(); });
    }
};
