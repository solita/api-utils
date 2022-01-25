var search = function(map, searchUrlFunction, searchInput, olstuff, select, unselect, peek, unpeek) {
    var searchStyles = [[0,255,0,0.4], [0,0,255,0.4], [255,0,0,0.4], [255,255,0,0.4], [0,255,255,0.4], [255,0,255,0.4]].map(function(c) {
        var stroke = [...c];
        stroke[3] = 1;
        return olstuff.styles.defaultWithColor(c,stroke);
    });

    window.onhashchange = function() {
        window.location.hash.split('#').filter(function(x) { return x != ''; }).map(function(h) { return decodeURIComponent(h); }).forEach(function(h,index) {
            let s = searchUrlFunction(h);
            if (s && map.getLayers().getArray().filter(function(x) {return x.getProperties().title == olstuff.mkLayerTitle(h,h);}).length == 0) {
                let layer = s instanceof Array ?
                     new ol.layer.Vector({source: new ol.source.Vector({features: [new ol.format.WKT().readFeature(s[0])] })}) :
                     olstuff.newVectorLayerNoTile(s, h, h, h, undefined, undefined, searchStyles[index % searchStyles.length]);
                layer.setVisible(true);
                map.addLayer(layer);
                layer.once('change', function(e) {
                    map.getView().fit(layer.getSource().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                });
                hover(map, [layer], peek, unpeek);
                click(map, [layer], select, unselect);
            }
        });
    }
    
    window.onhashchange();
    
    if (window.location.hash) {
        searchInput.value = decodeURI(window.location.hash.substring(1));
    }
    
    var searchChanged = function() {
        window.location.hash = '#' + encodeURI(searchInput.value);
    };
    
    searchInput.onchange = searchChanged;
    
    return searchChanged;
};
