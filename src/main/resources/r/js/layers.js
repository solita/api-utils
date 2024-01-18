var initLayers = function(map, olstuff, path) {
    if (path.indexOf(';') >= 0) {
        var ls = path.substring(path.lastIndexOf(';')+1).split(',');
        map.getLayers().getArray().map(olstuff.actualLayers).reduce(function(a,b) {return a.concat(b)}).forEach(function(l) {
            if (ls.includes(l.get('shortName'))) {
                l.setVisible(true);
            }
        });
        map.getControls().getArray().filter(function(x) { return x.showPanel; }).forEach(function(x) { x.showPanel(); x.hidePanel(); });
    }
};
