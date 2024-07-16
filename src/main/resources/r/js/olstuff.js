var olstuff = function(constants, util) {
    proj4.defs("EPSG:3067", "+proj=utm +zone=35 +ellps=GRS80 +units=m +no_defs");
    proj4.defs("EPSG:4326","+proj=longlat +datum=WGS84 +no_defs +type=crs");
    proj4.defs("EPSG:3857","+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs +type=crs");
    ol.proj.proj4.register(proj4);
    
    var ret = {
        projection: ol.proj.get('EPSG:3067'),
        
        geojsonProjection: ol.proj.get('EPSG:4326'),
        
        tileGrid: new ol.tilegrid.TileGrid({
            resolutions: constants.resolutions,
            tileSizes: constants.tileSizes,
            extent: constants.dataExtent
        }),
        
        tileGridAll: new ol.tilegrid.TileGrid({
            resolutions: constants.resolutions,
            extent: constants.dataExtent
        }),
        
        format: new ol.format.GeoJSON(),
        
        overlay: function (element) {
            return new ol.Overlay(({
                element: element,
                offset: [50,-50]
            }));
        },
        
        toWKT: function(feature) {
            return new ol.format.WKT().writeFeature(feature).replace(/[.]\d+/g, '');
        },
        
        createPolygonInteraction: function(map, callback) {
            var vector = new ol.layer.Vector({
                source: new ol.source.Vector(),
                style: new ol.style.Style({
                    fill: new ol.style.Fill({
                      color: 'rgba(255, 255, 255, 0.2)'
                    }),
                    stroke: new ol.style.Stroke({
                      color: '#ffcc33',
                      width: 2
                    }),
                    image: new ol.style.Circle({
                      radius: 7,
                      fill: new ol.style.Fill({
                        color: '#ffcc33'
                      })
                    })
                })
            });
            map.addLayer(vector);
            var draw = new ol.interaction.Draw({
                source: new ol.source.Vector({wrapX: false}),
                type: 'Polygon'
            });
            draw.on('drawend', function(e) {
                return callback(e, map.getCoordinateFromPixel([50,100]));
            });
            return draw;
        },
        
        createContextMenu: function(map, callback) {
            map.getViewport().addEventListener('contextmenu', function (evt) {
                evt.preventDefault();
                var coords = map.getEventCoordinate(evt);
                callback(evt, coords);
            });
        },
        
        actualLayers: function(mapOrGroup) {
            var results = [];
            if (mapOrGroup.getLayers) {
                mapOrGroup.getLayers().forEach(function(groupOrLayer) {
                    if (groupOrLayer.getLayers) {
                        results = results.concat(ret.actualLayers(groupOrLayer));
                    } else {
                        results.push(groupOrLayer);
                    }
                });
            }
            return results;
        },
        
        actualVisibleLayers: function(map) {
            return ret.actualLayers(map).filter(function(l) {
                return l.getVisible();
            });
        },
        
        featuresOnScreen: function(map) {
            var mapExtent = map.getView().calculateExtent(map.getSize());
            var results = {};
            var onlyUnique = function(value, index, self) { 
                return (!value.tunniste && !value.external_id) || self.findIndex(function(v) { return v.tunniste && v.tunniste == value.tunniste || v.external_id && v.external_id == value.external_id; }) === index;
            };
            ret.actualVisibleLayers(map).forEach(function(layer) {
                if (layer.getSource && layer.getSource().getFeaturesInExtent) {
                    var props = layer.getProperties();
                    results[props.title] = layer.getSource().getFeaturesInExtent(mapExtent).map(function(x) {
                        return util.withoutProp(util.withoutProp(x.getProperties(), 'geometry'), 'labelPoint');
                    }).filter(onlyUnique)
                      .sort(function(a,b) { return (a.osuus && b.osuus) ? (a.osuus - b.osuus) : (a.etappi && b.etappi) ? (a.etappi - b.etappi) : a.osuus ? -1 : b.osuus ? 1 : 0; });
                }
            });
            return results;
        },
        
        getFeatureByTunniste: function(map, tunniste) {
            var layers = ret.actualVisibleLayers(map);
            for (var i = 0; i < layers.length; i++) {
                var layer = layers[i];
                if (layer.getSource && layer.getSource().getFeatures) {
                    var features = layer.getSource().getFeatures();
                    for (var j = 0; j < features.length; ++j) {
                        if (features[j].getProperties().tunniste == tunniste || features[j].getProperties()._tunniste == tunniste || features[j].getProperties().external_id == tunniste) {
                            return features[j];
                        }
                    }
                } else if (layer.getSource && layer.getSource().getFeaturesInExtent) {
                    var features = layer.getSource().getFeaturesInExtent(constants.dataExtent);
                    for (var j = 0; j < features.length; ++j) {
                        if (features[j].getProperties().tunniste == tunniste || features[j].getProperties()._tunniste == tunniste || features[j].getProperties().external_id == tunniste) {
                            return features[j];
                        }
                    }
                }
            }
            return null;
        },
        
        registerListView: function(map, elem, selectInteraction, select, unselect, click) {
            if (!map.mystate) {
                map.mystate = {};
                map.mystate.closed = {};
            }
            document.onkeydown = function(e) {
                if (!e.ctrlKey && !e.metaKey && !document.querySelector('input:not(#rajoita):focus')) {
                    document.getElementById('rajoita').focus();
                }
            };
            var f = function(evt) {
                elem.innerHTML = '<input id="rajoita" autofocus type="text" placeholder="rajoita/restrict..." /><br />' + util.prettyPrint(util.withoutProp(util.withoutProp(ret.featuresOnScreen(map), 'geometry'), 'labelPoint'));
                util.initPrettyPrinted(elem);
                var input = elem.querySelector(':scope input');
                input.onkeyup = function() {
                    [...elem.querySelectorAll(':scope > ul > li > span > span > ul')].filter(function(x) { return x.textContent.indexOf(input.value) >= 0; }).forEach(function(x) { x.style.display = 'block'; });
                    [...elem.querySelectorAll(':scope > ul > li > span > span > ul')].filter(function(x) { return x.textContent.indexOf(input.value) < 0;  }).forEach(function(x) { x.style.display = 'none'; });
                };
                elem.querySelectorAll(':scope > * > * > .key').forEach(function(x) {
                    if (map.mystate.closed[x.textContent]) {
                        util.getSiblings(x).forEach(function(y) { y.style.display = 'none'; });
                    }
                });
                elem.querySelectorAll(':scope > * > * > .key').forEach(function(x) {
                    x.onmouseup = function() {
                        if (map.mystate.closed[x.textContent]) {
                            map.mystate.closed[x.textContent] = undefined;
                            util.getSiblings(x).forEach(function(y) { y.style.display = 'block'; });
                        } else {
                            map.mystate.closed[x.textContent] = true;
                            util.getSiblings(x).forEach(function(y) { y.style.display = 'none'; });
                        }
                    };
                });
                [...elem.querySelectorAll(':scope > ul > li > span > span > ul')].forEach(function(x) {
                    x.onmouseenter = function() {
                        [...x.querySelectorAll(':scope > li > .key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0 || y.textContent.indexOf("external_id") >= 0; })
                                                                     .slice(0, 1)
                                                                     .forEach(function(tunnisteElem) {
                            var tunniste = tunnisteElem.textContent == '_tunniste'
                                ? util.getSiblings(tunnisteElem).map(function(y) { return y.innerHTML; }).join()
                                : util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            var feature = ret.getFeatureByTunniste(map, tunniste);
                            try {
                                selectInteraction.getFeatures().clear();
                            } catch (e) {
                                console.log(e);
                            }
                            if (feature) {
                                selectInteraction.getFeatures().push(feature);
                                select(feature);
                            }
                        });
                    };
                    x.onmouseleave = function() {
                        var features = [...x.querySelectorAll(':scope > li > .key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0 || y.textContent.indexOf("external_id") >= 0; })
                                                                                    .slice(0, 1)
                                                                                    .map(function(tunnisteElem) {
                            var tunniste = tunnisteElem.textContent == '_tunniste'
                                ? util.getSiblings(tunnisteElem).map(function(y) { return y.innerHTML; }).join()
                                : util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            return ret.getFeatureByTunniste(map, tunniste);
                        }).filter(x => x);
                        if (features.length > 0) {
                            unselect(features);
                        }
                    };
                    x.onmouseup = function() {
                        [...x.querySelectorAll(':scope > li > .key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0 || y.textContent.indexOf("external_id") >= 0; })
                                                                     .slice(0, 1)
                                                                     .forEach(function(tunnisteElem) {
                            var tunniste = tunnisteElem.textContent == '_tunniste'
                                ? util.getSiblings(tunnisteElem).map(function(y) { return y.innerHTML; }).join()
                                : util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            var feature = ret.getFeatureByTunniste(map, tunniste);
                            if (feature) {
                                if (click) {
                                    click(feature);
                                }
                                map.getView().fit(feature.getGeometry().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                            }
                        });
                    };
                });
                
                elem.querySelectorAll(':scope > ul > li > span > span > ul a.oid').forEach(function(x) {
                    x.onmouseenter = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        try {
                            selectInteraction.getFeatures().clear();
                        } catch (e) {
                            console.log(e);
                        }
                        if (feature) {
                            selectInteraction.getFeatures().push(feature);
                        }
                        select(feature, null, tunniste);
                    };
                    x.onmouseleave = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        if (feature) {
                            unselect([feature]);
                        } else {
                            unselect([]);
                        }
                    };
                    x.onmouseup = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        if (feature) {
                            if (click) {
                                click(feature);
                            }
                            map.getView().fit(feature.getGeometry().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                        }
                        return false;
                    };
                });
            };
            map.on("moveend", f);
            f();
        },
        
        styles: {
            // from ol source
            defaultWithColor: function(fillColor, strokeColor) {
                return new ol.style.Style({
                    image: new ol.style.Circle({
                        fill: new ol.style.Fill({
                            color: fillColor
                            }),
                        stroke: new ol.style.Stroke({
                            color: strokeColor,
                            width: 1.25
                            }),
                        radius: 5
                    }),
                    fill: new ol.style.Fill({
                        color: fillColor
                    }),
                    stroke: new ol.style.Stroke({
                        color: strokeColor,
                        width: 1.25
                    })
                });
            },
            
            circle: function(radius, color) {
                var vcolor = ol.color.asArray(color);
                vcolor[3] = 1.0;
                return new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: radius,
                        fill: new ol.style.Fill({
                            color: color
                        }),
                        stroke: new ol.style.Stroke({
                            color: vcolor,
                            width: 1.0
                        })
                    })
                });
            },
            
            line: function(width, color) {
                return new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: color,
                        width: width
                    })
                });
            },
            
            onLoad: function(func) {
                return func;
            },
            
            icon: function(url, flipped, rotation, anchor, scale, opacity) {
                return new ol.style.Style({
                    image: new ol.style.Icon({
                        src: url,
                        scale: scale ? scale : 1,
                        rotateWithView: true,
                        anchor: anchor ? anchor : [0.5, 0.5],
                        rotation: rotation ? 2*Math.PI*rotation/360 : 0,
                        opacity: opacity ? opacity : 1.0
                    })
                });
            },
            
            text: function(color, txt, offsetY) {
                return new ol.style.Style({
                    text: new ol.style.Text({
                        text: txt,
                        stroke: new ol.style.Stroke({
                            color: color
                        }),
                        fill: new ol.style.Fill({
                            color: color
                        }),
                        offsetY: offsetY || 0
                    })
                });
            }
        },
        
        newVectorLayerNoTile: function(url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames, simplify, cql_filter) {
            return ret.newVectorLayerImpl(false, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames, simplify, cql_filter);
        },
        
        newVectorLayer: function(url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames, simplify, cql_filter) {
            return ret.newVectorLayerImpl(true, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames, simplify, cql_filter);
        },
        
        newVectorLayerImpl: function(tiling, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames, simplify, cql_filter) {
            var u1 = url + (url.indexOf('?') < 0 ? '?' : '');
            u1 = u1.indexOf('.geojson') < 0 && !u1.startsWith('http') && !u1.includes('mml/') ? u1.replace('?', '.geojson?') : u1;
            var instant = new URLSearchParams(window.location.search).get('time');
            var u2 = (window.location.search.indexOf('profile') == -1 ? '' : '&profile=true') +
                     (!cql_filter ? '' : '&cql_filter=' + cql_filter) +
                     (!propertyName ? '' : '&propertyName=' + propertyName) +
                     (!simplify || url.indexOf('simplify=') >= 0 ? '' : '&simplify=' + simplify) +
                     (url.indexOf('time=') >= 0 || !instant ? '' : '&time=' + instant + '/' + instant) +
                     (!typeNames ? '' : '&typeNames=' + typeNames);

            var layerTitle = ret.mkLayerTitle(title_fi, title_en);
            var aborter = new AbortController();
            var layer;
            var source = new ol.source.Vector({
                format: ret.format,
                projection: ret.projection,
                strategy: tiling ? ol.loadingstrategy.tile(ret.tileGrid) : ol.loadingstrategy.all,
                loader: function(extent, resolution, projection) {
                    if (tiling && (extent[0] < constants.dataExtent[0] ||
                                   extent[1] < constants.dataExtent[1] ||
                                   extent[2] > constants.dataExtent[2] ||
                                   extent[3] > constants.dataExtent[3])) {
                        return;
                    }
                    var kaavio = document.getElementById('kaavio');
                    layer.dispatchEvent("loadStart");
                    fetch((u1 + (tiling ? '&bbox=' + extent.join(',') : '') + (kaavio && kaavio.checked ? '&presentation=diagram' : '') + u2).replace('?&','?'), {signal: aborter.signal})
                        .then(function(response) { if (response.ok) { return response.json(); } else { throw new Error(response.status + ": " + response.statusText); } })
                        .then(function(response) {
                          layer.dispatchEvent("loadSuccess");

                          // openlayers throws error if coordinates == null
                          if (response.type == 'FeatureCollection') {
                              response.features.filter(f => f.geometry && f.geometry.coordinates == null).forEach((f => f.geometry.coordinates = []));
                          } else if (response.type == 'Feature') {
                              if (response.geometry && response.geometry.coordinates == null) {
                                  response.geometry.coordinates = [];
                              }
                          }

                          var features = ret.format.readFeatures(response);
                          
                          // populate resolved features with properties from the corresponding "parent" object
                          var resolved = features.filter(f => f.getProperties()._resolved == true);
                          var direct = features.filter(f => !f.getProperties()._resolved);
                          resolved.forEach(resolvedFeature => {
                              var resolvedProps = resolvedFeature.getProperties();
                              var parts = resolvedProps._source.split('.');
                              if (resolvedProps.tunniste && resolvedProps._source) {
                                var x = direct.find(d => {
                                    var directProps = parts.reduce((acc,cur) => acc instanceof Array ? acc.map(a => a[cur]) : acc[cur], d.getProperties());
                                    return directProps == resolvedProps.tunniste || directProps instanceof Array && directProps.includes(resolvedProps.tunniste);
                                });
                                if (x) {
                                    // "geometry" is also a property due to some bad API design... so explicitly keep the actual geometry.
                                    resolvedFeature.setProperties({...resolvedProps, ...x.getProperties(), geometry: resolvedProps.geometry});
                                }
                              }
                          });
                          
                          features.forEach(f => {
                            if (!f.getProperties().tunniste) {
                                var newProps = f.getProperties();
                                newProps._tunniste = newProps.osuus ? newProps.osuus : newProps.etappi ? newProps.etappi : layerTitle;
                                f.setProperties(newProps);
                            }
                          });
                          if (styleOrHandler instanceof Function) {
                              if (styleOrHandler.length == 1) {
                                  // on feature load
                                  features.forEach(styleOrHandler);
                              } else {
                                  // dynamic style
                                  features.forEach(function(f) { f.setStyle(styleOrHandler); });
                              }
                          }
                          source.addFeatures(features);
                        })
                        .catch(err => {
                            if (err.name == 'AbortError') {
                                layer.dispatchEvent("loadAbort");
                            } else {
                                console.log(err);
                                layer.dispatchEvent("loadFail");
                            }
                        });
                }
            });
            source.on('clear', () => {
                aborter.abort();
                aborter = new AbortController();
            });
            layer = new ol.layer.Vector({
                title: layerTitle,
                shortName: shortName,
                source: source,
                opacity: opacity || 1.0,
                style: styleOrHandler instanceof Function ? undefined : styleOrHandler,
                extent: constants.dataExtent,
                renderBuffer: constants.renderBuffer,
                updateWhileInteracting: true,
                updateWhileAnimating: true
            });
            layer.setVisible(false);
            let update = diff => {
                let e = document.querySelector('.layer-switcher');
                e.setAttribute('data-loading', parseInt(e.getAttribute('data-loading') || '0') + diff);
            };
            
            layer.on('loadStart', () => update(1));
            layer.on('loadSuccess', () => update(-1));
            layer.on('loadFail', () => update(-1));
            layer.on('loadAbort', () => update(-1));
            
            layer.on('loadStart', () => { layer.setVisible(true); });
            layer.on('loadFail',  () => { layer.setVisible(false); });
            layer.on('loadAbort', () => { layer.setVisible(false); });
            
            return layer;
        },
        
        mkLayerTitle: function(title_fi, title_en) {
            return '<span lang="fi">' + ret.modifyLayerTitle(title_fi) + '</span><span lang="en">' + ret.modifyLayerTitle(title_en) + '</span>';
        },
        
        modifyLayerTitle: function(title) {
            if (title.indexOf('=:') > 0 || title.indexOf('*:') > 0) {
                return title.replaceAll(',', ', ');
            }
            return title;
        },
        
        newPngLayer: function(url, title, opacity, propertyName, typeNames) {
            var u1 = url + '.png?';
            var instant = new URLSearchParams(window.location.search).get('time');
            var u2 = (window.location.search.indexOf('profile') == -1 ? '' : '&profile=true') + (propertyName === null ? '' : '&propertyName=' + propertyName) + '&time=' + instant + '/' + instant;
            u2 += (typeNames ? '&typeNames=' + typeNames : '');

            var source = new ol.source.TileImage({
                format: ret.format,
                projection: ret.projection,
                tileGrid: ret.tileGridAll,
                tileUrlFunction: function(tileCoord, resolution, projection) {
                    var extent = source.tileGrid.getTileCoordExtent(tileCoord);
                    if (extent[0] < constants.dataExtent[0] ||
                        extent[1] < constants.dataExtent[1] ||
                        extent[2] > constants.dataExtent[2] ||
                        extent[3] > constants.dataExtent[3]) {
                        return;
                    }
                    return u1 + 'bbox=' + extent.join(',') + u2;
                }
            });
            var layer = new ol.layer.Tile({
                title: title,
                source: source,
                opacity: opacity || 1.0,
                extent: constants.dataExtent
            });
            layer.setVisible(false);
            return layer;
        },
        
        newImageLayer: function(url, title, opacity) {
            var layer = new ol.layer.Image({
                title: title,
                opacity: opacity || 1.0,
                extent: constants.dataExtent,
                source: new ol.source.ImageStatic({
                  url: url,
                  projection: ret.projection,
                  imageExtent: constants.dataExtent,
                })
              });
            layer.setVisible(false);
            return layer;
        },
        
        newWMTSGroup: function(title_fi, title_en, url, matrix) {
            var parser = new ol.format.WMTSCapabilities();
            var group = new ol.layer.Group({
                title: ret.mkLayerTitle(title_fi, title_en),
                layers: [],
                fold: 'close'
            });
            
            var createLayer = function(matrix) { return function(response) {
                var result = parser.read(response);
                result.Contents.Layer.map(function(l) {
                    var id = l.Identifier;
                    var tms = l.TileMatrixSetLink.map(function(t) { return t.TileMatrixSet; });
                    if (tms.indexOf(matrix) > -1) {
                        var options = ol.source.WMTS.optionsFromCapabilities(result, {layer: id, matrixSet: matrix});
                        group.getLayers().push(ret.tileLayer(id, new ol.source.WMTS(options), 1.0));
                    }
                });
              };
            };
            
            fetch(url).then(function(response) { if (response.ok) { return response.text(); } else { throw new Error(response.status + ": " + response.statusText); } })
                      .then(createLayer(matrix))
                      .catch(console.log);
            
            return group;
        },
        
        taustaGroup: function(opacity, baseUrl) {
            var parser = new ol.format.WMTSCapabilities();
            var osm = ret.tileLayer('OpenStreetMap', new ol.source.OSM());
            osm.setVisible(true);
            var group = new ol.layer.Group({
                title: ret.mkLayerTitle('Taustat', 'Backgrounds'),
                layers: [ret.newImageLayer('https://placehold.it/256/eeffee?text=+', 'Green', opacity),
                         ret.tileLayer('Grid', new ol.source.TileImage({projection: ret.projection, tileGrid: ret.tileGrid, tileUrlFunction: function(extent, resolution, projection) {var size = ret.tileGrid.getTileSize(extent[0]); return 'https://placehold.it/' + size + '?text=' + extent + ' (' + size + 'x' + size + ')&w=' + size + '&h=' + size + '';} }), opacity),
                         ret.tileLayer('Debug', new ol.source.TileDebug({projection: ret.projection, tileGrid: ret.tileGrid}, opacity)),
                         osm
                        ],
                fold: 'close'
            });
            
            var createLayer = function(matrix, host) { return function(response) {
                var result = parser.read(response);
                result.Contents.Layer.map(function(l) {
                    var id = l.Identifier;
                    var tms = l.TileMatrixSetLink.map(function(t) { return t.TileMatrixSet; });
                    if (tms.indexOf(matrix) > -1) {
                        var options = ol.source.WMTS.optionsFromCapabilities(result, {layer: id, matrixSet: matrix});
                        if (host !== null) {
                            options.urls[0] = options.urls[0].replace(/[-a-z]+\.[a-z]+\.local/, host)
                                                             .replace(/\d+\.\d+\.\d+\.\d+/, host)
                                                             .replace(/[^.]+.maanmittauslaitos.fi/, host);
                        }
                        group.getLayers().push(ret.tileLayer(id, new ol.source.WMTS(options), opacity));
                    }
                });
              };
            };
            
            var maasto     = baseUrl + '/maasto/wmts/1.0.0/WMTSCapabilities.xml';
            var teema      = baseUrl + '/teema/wmts/1.0.0/WMTSCapabilities.xml';
            var kiinteisto = baseUrl + '/kiinteisto/wmts/1.0.0/WMTSCapabilities.xml';
            fetch(maasto)    .then(function(response) { if (response.ok) { return response.text(); } else { throw new Error(response.status + ": " + response.statusText); } })
                             .then(createLayer('ETRS-TM35FIN', baseUrl))
                             .catch(console.log);
            fetch(teema)     .then(function(response) { if (response.ok) { return response.text(); } else { throw new Error(response.status + ": " + response.statusText); } })
                             .then(createLayer('ETRS-TM35FIN', baseUrl))
                             .catch(console.log);
            fetch(kiinteisto).then(function(response) { if (response.ok) { return response.text(); } else { throw new Error(response.status + ": " + response.statusText); } })
                             .then(createLayer('ETRS-TM35FIN', baseUrl))
                             .catch(console.log);
            
            return group;
        },
        
        maastotiedotGroup: function(baseUrl) {
            var collectionsUrl = baseUrl + "/maastotiedot/features/v1/collections";
            var group = new ol.layer.Group({
                title: ret.mkLayerTitle('Maastotiedot','Topographic'),
                layers: [],
                fold: 'close'
            });
            
            fetch(collectionsUrl + '?f=json').then(function(response) { if (response.ok) { return response.json(); } else { throw new Error(response.status + ": " + response.statusText); } })
                                 .then(function(x) {
                var cols = x.collections;
                cols.sort(function(a,b) { return a.id < b.id; })
                    .forEach(function(c) {
                    var title = c.title;
                    // MML added default limit of 100...
                    var url = collectionsUrl + "/" + c.id + "/items?crs=http://www.opengis.net/def/crs/EPSG/0/3067&f=json&limit=10000";
                    var layer = ret.newVectorLayerNoTile(url, c.id, title, title);
                    group.getLayers().push(layer);
                });
            })
            .catch(console.log);
            
            return group;
        },
        
        tileLayer: function(title, source, opacity) {
            var layer = new ol.layer.Tile({
                title: title,
                opacity: opacity || 0.3,
                source: source,
                renderBuffer: constants.renderBuffer,
                updateWhileInteracting: true,
                updateWhileAnimating: true
            });
            layer.setVisible(false);
            return layer;
        },
        
        map: function(overlays, layers, taustaGroup, renderer) {
            return new ol.Map({
                target: 'kartta',
                overlays: overlays,
                layers: layers.concat(taustaGroup),
                renderer: renderer || 'canvas',
                view: new ol.View({
                    center: [342900, 6820390],
                    resolution: 2048,
                    resolutions: constants.resolutions,
                    projection: ret.projection
                }),
                controls: [
                    new ol.control.Attribution({collapsible: false}),
                    new ol.control.Zoom(),
                    new ol.control.ZoomSlider(),
                    new ol.control.Rotate(),
                    new RotateLeftControl(),
                    new RotateRightControl(),
                    new ol.control.ScaleLine(),
                    new ol.control.MousePosition({
                        coordinateFormat: function(c) {
                            return Math.round(c[0]) + "," + Math.round(c[1]);
                        }
                    }),
                    new ol.control.LayerSwitcher({ groupSelectStyle: 'children' })
                ]
            });
        }
    };
    
    ol.proj.addProjection(ret.projection);
    return ret;
};
