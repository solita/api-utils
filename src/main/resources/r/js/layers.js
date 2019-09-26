var initLayers = function(map, olstuff) {
    var ls = window.location.pathname.substring(window.location.pathname.lastIndexOf(';')+1).split(',');
    map.getLayers().getArray().map(olstuff.actualLayers).reduce(function(a,b) {return a.concat(b)}).forEach(function(l) {
        if (ls.includes(l.get('shortName'))) {
            l.setVisible(true);
        }
    });
};
