package fi.solita.utils.api.html;

import static fi.solita.utils.api.html.UI.concat;
import static fi.solita.utils.api.html.UI.definition_en;
import static fi.solita.utils.api.html.UI.definition_fi;
import static fi.solita.utils.api.html.UI.link;
import static fi.solita.utils.api.html.UI.text;
import static fi.solita.utils.functional.Functional.mkString;
import static org.rendersnake.HtmlAttributesFactory.href;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fi.solita.utils.api.types.Filters;

public abstract class FAQ implements Renderable {
    public static Renderable page(String copyright_fi, String copyright_en) {
        return new Page("FAQ", "FAQ", copyright_fi, copyright_en, new FAQ() {});
    }
    
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
          html.section()
              .dl().render(definition_fi(
"Miksi Swagger-dokumentaatiossa on puutteita?",
"Dokumentaatio generoidaan koodista valmiilla työkalulla, joka toimii melko epävarmasti. Rajapintakuvauksen pitäisi kuitenkin olla kunnossa. Mikäli jokin swagger-kuvauksen vika ei ole jäänyt testeissämme kiinni, kerrothan meille siitä niin korjataan."
                 )).render(definition_en(
"Why are there imperfections in Swagger documentation?",
"The documentation is generated from program code with a tool, which is a bit unstable. The API-description should still be correct. If you find a problem in swagger descriptions that hasn't been caught in our tests, please let us know and we'll fix it."
                 ))

                   .render(definition_fi(
"Miksi Swagger-käyttöliittymä näyttää Map-paluuarvoisille metodeille tyhjän mallin ja esimerkin?",
"Tämä on bugi swagger-ui-työkalussa, ja on korjattu seuraavaan major-versioon. Itse rajapintakuvaus on kuitenkin kunnossa, joten ongelma on kosmeettinen. Asia korjaantuu kun käyttämämme kirjasto siirtyy tukemaan uudempaa swagger-ui-versiota."
                 )).render(definition_en(
"Why does the Swagger UI show an empty model and example for methods returning a Map?",
"This is a bug in Swagger-UI tool, and should be fixed in the next major version. The API description itself is still fine, so the problem is cosmetic. This will be fixed when the library we use starts to use a more recent Swagger-UI release."
                 ))

                   .render(definition_fi(
"Miksi datassa olevat tunnisteet ovat sellaista muotoa kuin ovat?",
"Tunnisteen muodosta ei pidä välittää, vaan ne pitää aina käsitellä mielivaltaisina merkkijonoina. Rajapinnan palauttamat tunnisteet ovat globaalisti uniikkeja."
                 )).render(definition_en(
"Why are identifiers in the data of the format they are?",
"Identifiers should always be handled as opaque strings. The identifiers returned by the API are globally unique."
                 ))

                   .render(definition_fi(
"Säilyvätkö vanhat rajapintaversiot saatavilla?",
"Kyllä. Joskus niitä kuitenkin todennäköisesti tullaan poistamaan, joten pyrithän käyttämään uusinta saatavillaolevaa versiota."
                 )).render(definition_en(
"Will all the old API versions be kept available?",
"Yes. Although most likely some day the oldest ones are going to be removed, so please always try to use as recent version as possible."
                 ))

                   .render(definition_fi(
"Säilyvätkö vanhat rajapintaversiot muuttumattomina?",
"Rajapintalupauksena kyllä. Korjauksia niihin kuitenkin tehdään. Lisäyksiä saattaa myös tulla, esimerkiksi kokonaan uusi rajapintametodi, tai uusi vastausmuoto, tai uusi kenttä olemassaolevaan paluuarvoon."
                 )).render(definition_en(
"Will all the old API versions be kept unmodified?",
"As an API promise, yes. However, corrections and bug fixes are made. Old versions might also get some additions, like a new API method, or a new response format, or a new field to an existing return value."
                 ))

                   .render(definition_fi(
"Missä eri muodoissa rajapinnan data on tarjolla?",
"Swagger-dokumentaatiosta näet ajantasaisen listan. Lähes kaikki rajapintametodit on saatavilla kaikissa vastausmuodoissa."
                 )).render(definition_en(
"In what formats is the data available?",
"Swagger documentation will show the available formats. Almost all API methods can produce all available formats."
                 ))

                   .render(definition_fi(
"Miten saan piirretyä dataa kartalle?",
"Voit katsoa rajapinnan etusivun karttatoteutuksesta mallia."
                 )).render(definition_en(
"How can I draw some data on a map?",
"You can use the API fronpage map implementation as an example."
                 ))

                   .render(definition_fi(
"Etusivun karttatoteutus on melko iso, vaikka onkin melko yksinkertainen. Miten saan edes jotakin kartalle?",
link(href("minimal.html"), text("Tässä minimaalinen OpenLayers-esimerkki."))
                 )).render(definition_en(
"API frontpage map implementation is quite large, even if quite simple. How can I get at least something on a map?",
link(href("minimal.html"), text("Here's a minimal OpenLayers-example."))
                 ))

                   .render(definition_fi(
"Mikä on 'time'-parametri?",
"Parametri määrittää ajanhetken tai aikavälin, jota voimassaololtaan leikkaavat rivit palautetaan. Yleisin tarve on 'nykyhetki', joksi suosittelemme käytettävän aina edellistä UTC-ajan keskiyötä (T00:00:00Z) välimuistiosuvuuden parantamiseksi."
                 )).render(definition_en(
"What is 'time' parameter?",
"The parameter defines the instant or the interval determining which rows are returned. Those whose validity intersects with the value are returned. Most commond need would be 'now', for which the preceding UTC midnight (T00:00:00Z) is recommended to improve cache hit rate."
                 ))

                   .render(definition_fi(
"Miksi aikaleimoissa on joskus perässä Z ja joskus ei, mitä aikoja nämä ovat?",
"Rajapinta palauttaa aikaleimat UTC-ajassa (Z), ja jättää aikavyöhykemuunnokset käyttäjän harteille. Ihmisen luettavaksi tarkoitetuissa vastausmuodoissa (HTML, CSV, XLSX) kuitenkin palautetaan aikaleimat Suomen aikavyöhykkeessä sekaannusten välttämiseksi."
                 )).render(definition_en(
"Why do timestamps sometimes have a trailing Z and sometimes not? What times are these?",
"The API returns timestamps in UTC (Z) thus leaving possible time zone handling for the user. Except in formats meant primarily for human consumption (HTML, CSV, XLSX) the stamps are in Finland's timezone to prevent mixups."
                 ))

                   .render(definition_fi(
"Pitääkö minun rajoittaa propertyName-parametrilla data aina mahdollisimman pieneksi?",
"Se riippuu. Osa datasta on raskaampi hakea tai laskennallista, joten sinänsä kannattaa pyytää vain se mitä tarvitsee. Toisaalta, välimuistiosuvuuden parantamiseksi kannattaa varioida parametreja mahdollisimman vähän. Jos haluat lähes kaiken datan, kannattaa todennäköisesti olla rajoittamatta sitä."
                 )).render(definition_en(
"Do I need to use propertyName-parameter to limit the response to as small as possible?",
"It depends. Some of the data is heavy to acquire or calculated in real time, so often you might want to ask only what you need. On the other hand, to improve cache hit ratio, as common set of parameter should be used as possible. If you need almost all the data, you probably should not limit it."
                 ))

                   .render(definition_fi(
"Mitä arvoja propertyName-parametri hyväksyy?",
"Karkeastiottaen ne, jotka näkyvät kyseisen rajapintametodin HTML-muodon sarakeotsikkoina. Ymmärtää myös hierarkioita, negaatiota ja *-wildcardia."
                 )).render(definition_en(
"What values does propertyName parameter accept?",
"Roughly speaking, the values visible in table header of the endpoint HTML format. Also understands hierarchies, negation and *-wildcard."
                 ))

                   .render(definition_fi(
"Useimmissa vastauksissa sallitaan ikuinen välimuistitus, voinko luottaa tähän?",
"Kyllä, cache-control-headereihin voi luottaa, ja voit näinollen halutessasi laittaa kutsujen väliin oman välimuistipalvelimen mikäli koet sen hyödylliseksi käyttötapaukseesi."
                 )).render(definition_en(
"Most responses are allowed to be cached eternally. Can I trust this?",
"Yes, you should trust cache-control headers. Thus you can easily add a transparent caching proxy between you and the API if you find it useful."
                 ))

                   .render(definition_fi(
"Miksi rajapinta tekee paljon redirectejä?",
"Välimuistituksen parantamiseksi. Tekemällä uudelleenohjauksia voidaan varsinaiset dataa palauttavat vastaukset välimuistittaa useimmiten ikuisesti."
                 )).render(definition_en(
"Why does the API make a lot of redirects?",
"To improve caching. To change the target of a redirect whenever the data is somehow changed, the responses returning the actual data can mostly be cached eternally."
                 ))

                   .render(definition_fi(
"Mikä on urlin alkuun ilmestyvä numerosarja?",
"Se kuvaa tietokannan datan jonkinlaista versionumeroa. Mikäli numero ei muutu, ei datakaan ole päivittynyt tietokannassa. Tätä numeroa EI kuulu liittää mukaan varsinaisiin kutsuttaviin rajapintametodeihin, vaan niihin siirrytään uudelleenohjauksen kautta. Paitsi jos haluat optimoida pois joitakin uudelleenohjauksia, ja hoidat datan päivittymisen tarkastelut manuaalisesti, missä tapauksessa numerosarjaa tulisi käsitellä mielivaltaisena merkkijonona."
                 )).render(definition_en(
"What's the sequence of numbers appearing in the beginning of the API relative URI path?",
"It describes a kind of a version number of the data in the database. As long as the number doesn't change, the data in the database also stays the same. This number should NOT be included in actual requests, they should only appear as redirect targets. Unless you want to optimize some redirects away and manually observe changes updating the number when needed, in which case the number should be treated as an opaque string."
                 ))

                   .render(definition_fi(
"Näenkö bi-temporaalisuuden toista historiaa (transaktioaika) jos muutan URL:n alkuun ilmestyvää numeroa?",
"Vain niiltä osin mitä sattuu säilymään jossakin HTTP-välimuistissa. Vaikka rajapinta kertookin historian (validisuusaika), ei se ole bi-temporaalinen."
                 )).render(definition_en(
"Can I observe the 'other' history (transaction time) of bi-temporality if I change the number appearing in the URL?",
"Only those parts that happen to be stored in a HTTP cache somewhere. Even though the API knows history (valid time), it's not bi-temporal."
                 ))

                   .render(definition_fi(
"Mitä tarkoittaa bi-temporaalisuus?",
"Se ei mahdu tämän FAQ:n scopeen."
                 )).render(definition_en(
"What is bi-temporality?",
"It's out of scope for this FAQ."
                 ))

                   .render(definition_fi(
"Miksi monet JSON-vastaukset tulevat objektina eivätkä listana?",
"Kyseltäessä yksittäisellä ajanhetkellä, sisältävät avaimen alla olevat listat aina vain yhden rivin, joka kuvaa miltä käsite näyttää kyseisellä ajanhetkellä. Kyseltäessä aikavälillä, voi tuo lista sisältää useita rivejä mikäli käsite muuttuu jotenkin pyydetyllä aikavälillä. Objekti-rakenne takaa, että yhteen käsitteeseen liittyvät rivit löytyvät aina vastaavan avaimen alta, eikä sinun tarvitse varautua siihen että taulukossa olisi joskus useampia saman käsitteen rivejä. Mikäli koet että tämä aiheuttaa ongelmia esimerkiksi tiettyjen työkalujen toimivuuden suhteen, ota yhteyttä niin mietitään jotakin."
                 )).render(definition_en(
"Why are many JSON responses Maps instead of just Lists?",
"When querying with a single instant of time, the map keys should always containt a singleton list describing how the object looks on that instant. When querying with an interval, can that list include multiple rows if the row happens to change during that interval. Map structure allows all the rows related to the single object to be found under the single key, and you don't have to consider the case when multiple rows of a single list refer to the same object identity. If you find this troublesome for example related to some specific tooling, please let us know and we'll think of something."
                 ))

                   .render(definition_fi(
"Miksi pyynnöissä on niin tiukat vaatimukset, kuten parametrien aakkosjärjestys?",
"Välimuistiosuvuuden parantamiseksi. Mikäli koet että tämä aiheuttaa ongelmia esimerkiksi tiettyjen työkalujen toimivuuden suhteen, ota yhteyttä niin mietitään jotakin."
                 )).render(definition_en(
"Why are there such strict requirements for requests, like the alphabetical order of query parameters?",
"To improve cache hit ratio. If you find this troublesome for example related to some specific tooling, please let us know and we'll think of something."
                 ))

                   .render(definition_fi(
"Missä koordinaattijärjestelmässä geometriat ovat?",
"Oletuksena ETRS-TM35FIN-karttaprojektio ja -tasokoordinaatisto (EPSG:3067)."
                 )).render(definition_en(
"In which coordinate system are the geometries in?",
"By default, ETRS-TM35FIN projection and plain coordinate system (EPSG:3067)"
                 ))

                   .render(definition_fi(
"Mitä muita koordinaattijärjestelmiä on tarjolla?",
"Saat vastaukset myös WGS84-muodoissa antamalla parametrin srsName=epsg:4326 tai srsName=crs:84 tai srsName=epsg:3857. Tällöin myös mahdolliset koordinaatteja sisältävät kyselyparametrit olettavat saavansa vastaavaa muotoa. 'bbox'-parametrin koordinaattijärjestelmän voit määrätä erikseen viidennellä arvolla."
                 )).render(definition_en(
"What other coordinate systems are available?",
"The responses are available also in different WGS84 formats specified by query parameter srsName=epsg:4326 or srsName=crs:84 or srsName=epsg:3857. Then also possible coordinate values in request parameters are assumed to be in this format. The format of 'bbox'-parameter can be specified separately with a fifth value."
                 ))

                   .render(definition_fi(
"Miten päin WGS84:n longitude ja latitude ovat koordinaateissa?",
concat(text("Tämä rajapinta välttää paikkatietomaailman ongelman antamalla käyttäjän valita. srsName=epsg:4326 määrää järjestyksen lat,lon kun taas srsName=crs:84 päinvastaiseen järjestykseen. Sekasotkusta voi lukea lisää vaikkapa "), link(href("http://docs.geotools.org/latest/userguide/library/referencing/order.html"), text("täältä.")))
                 )).render(definition_en(
"Which way in WGS84 are longitude and latitude given?",
concat(text("This common problem is avoided by letting the user decide. srsName=epsg:4326 puts them in lat,lon while srsName=crs:84 in the opposite order. The problem is described in for example "), link(href("http://docs.geotools.org/latest/userguide/library/referencing/order.html"), text("here.")))
                 ))

                   .render(definition_fi(
"Vektorit vai rasteroidut kuvat?",
"Oma valintasi. Kuvat voivat olla suorituskykyisempiä erityisesti mobiililaitteissa, mutta vektorien kanssa voi vuorovaikuttaa. Vektorit ovat aina tarkkoja kaikilla zoom-tasoilla, kun taas kuvat pitää ladata erikseen tarkemmille zoom-tasoille. Yksi mahdollisuus on ladata 'korkeilla' zoom-tasoilla tasot kuvina ja vaihtaa vektoreihin kun lähestytään zoom-tasoa, jolla käyttäjän voisi olettaa haluavan vuorovaikuttaa kyseisen datan kanssa."
                 )).render(definition_en(
"Vectors or rasterized images?",
"Your choice. Images may be more performant especially in mobile devices, but vectors allow interaction. Vectors are always sharp in every zoom level, whereas images need to be loaded separately for more lower zoom levels. One possibility would be to load images on high zoom levels, and change to vectors when approaching a lower zoom level where the user might want to interact with the data in question."
                 ))
                   
                   .render(definition_fi(
"Miksi rasteroidut kuvat (WMTS/PNG) eivät aina vastaa vektoroituja?",
"Tällä hetkellä kuvien renderöinti ei huomioi kaikkia oleellisia ominaisuuksia. Esimerkiksi baliisin tyyppiä ei huomioida vaan kaikki baliisit piirretään käyttäen samaa SVG-ikonia baliisityypistä riippumatta."
                 )).render(definition_en(
"Why don't rasterized images (WMTS/PNG) always correspond exactly to vectorized?",
"Currently the rendering doesn't take into consideration all the properties. For example, balise type is not considered and instead all the balises are rendered using the same SVG icond irrespective of balise type."
                 ))

                   .render(definition_fi(
"Mitä 'cql_filter'-parametri tekee?",
"Suodattaa tulosjoukon rivejä ehtojen perusteella. Useita ehtoja voi yhdistää AND-sanalla. Useita AND-lohkoja voi yhdistää OR-sanalla. Toteutettuna osajoukko ECQL-filttereistä: " + mkString(", ", Filters.SUPPORTED_OPERATIONS) + ". INTERSECTS vaatii ensimmäiseksi parametriksi geometria-propertyn ja toiseksi POLYGON-tyyppisen WKT-geometrian."
                 )).render(definition_en(
"What does 'cql_filter'-parameter do?",
"Filter result rows based on given conditions. Multiple conditions can be combined with AND-keyword. Multiple AND-blocks can be combined with OR-keyword. A subset of ECQL-filters are implemented: " + mkString(", ", Filters.SUPPORTED_OPERATIONS) + ". INTERSECTS requires a geometry property as its first value and a literal WKT polygon as the second."
                 ))
                   
                   .render(definition_fi(
"Mitä funktioita 'cql_filter' ja 'propertyName' tukevat?",
"Ymmärtää rajoitetuissa paikoissa funktioita 'round', 'start', 'end' ja 'duration' sekä aritmetiikkaoperaatioita '+', '-', '*', '/'. 'start' ja 'end' voivat tietokenttien lisäksi viitata request-parametriin."
                 )).render(definition_en(
"What functions do 'cql_filter' and 'propertyName' support?",
"Understands the following functions in limited locations: 'round', 'start', 'end' and 'duration' as well as arithmetic operators '+', '-', '*', '/'. 'start' and 'end' can refer to a request parameter in addition to data fields."
                 ))

                   .render(definition_fi(
"Mistä saan WKT-muotoisia polygoneja?",
"Voit piirtää etusivun kartalle polygonin, ja saat sen WKT-muodossa."
                 )).render(definition_en(
"How can I get polygons in WKT format?",
"You can use the API frontpage map to draw a polygon and get its WKT format."
                 ))

                   .render(definition_fi(
"Miten piirrän infran kaaviomuodossa / mitä ovat kaaviogeometriat?",
"Käsitteillä on todellisten koordinaattisijaintien lisäksi 'kaaviokoordinaattisijainnit', joita käyttämällä data voidaan piirtää kaaviomaisena. Esitystapa voidaan valita 'presentation' parametrilla."
                 )).render(definition_en(
"How can I present the data in schematic format / what are schematic geometries?",
"In addition to physical coordinates every geometric object has 'schematic coordinates'. These can be used to draw the data in a schematic representation by using 'presentation' query parameter."
                 ))

                   .render(definition_fi(
"Miten haen 'sisäkkäistä' dataa, esimerkiksi Tilirataosat yhdessä kunnossapitoalueen nimen kanssa?",
"Voit jatkaa propertyName-arvoja 'suhde'-tapauksissa arvon sisään, esimerkiksi 'propertyName=kunnossapitoalue.nimi'. Ei käytettävissä kaikilla vastausmuodoilla. Tämä soveltuu erityisesti ad-hoc tarpeisiin tai yksittäisen objektin laajempien tietojen kyselemiseen. Rajapintaa aktiivisesti käyttävän sovelluksen voi olla parempi hakea datat erikseen ja yhdistelemällä tarpeen mukaan omassa päässään, hyötyäkseen paremmasta cache-osuvuudesta ja pienemmistä vasteajoista."
                 )).render(definition_en(
"How can I retrieve 'nested' data, like Accounting Railway Sections together with the name of their Maintenance area?",
"You can append nested structures to propertyName parameter values in the cases where there's a relation. For example: 'propertyName=kunnossapitoalue.nimi'. This is not available for all response formats. This is especially useful for ad-hoc needs or querying more data for a single object. Applications using the API actively are likely better to fetch different datasets separately and combine them as needed in their side, to utilize better cache hit ratio and lower response times."
                 ))

                   .render(definition_fi(
"Miten haen sarakkeista 'kaikki paitsi X'?",
"Voit jättää sarakkeita pois lisäämällä 'propertyName'-parametrissa annettuun nimeen väliviivan etumerkiksi. Tämä toimii myös 'sisäkkäisille' arvoille, mutta huomaa laittaa etumerkki sisäkkäisen nimen yhteyteen."
                 )).render(definition_en(
"How can I retrieve properties 'all but X'?",
"You can exclude properties by prefixing the names in 'propertyName'-parameter with a hyphen. This also works for 'nested' data but remember to add the hyphen to the nested part of the name."
                 ))

                   .render(definition_fi(
"Miten saan rajapinnasta haettua Liikennepaikkojen ja Liikennepaikkavälien muodostaman graafin?",
concat(text("Esimerkiksi "), link(href("lp-graph.txt"), text("näin")))
                 )).render(definition_en(
"How can I produce a picture representing the graph of stations (Liikennepaikka) and Station intervals (Liikennepaikkaväli)?",
concat(text("For example like "), link(href("lp-graph.txt"), text("this")))
                 ))
                   
                   .render(definition_fi(
"Miksi jotkut otsikot HTML-muodossa ovat punaisia?",
"Punaiset kentät ovat laskennallista dataa, eli mahdollisesti raskaampia muodostaa. Nämä kannattaa jättää ottamatta jos et niitä tarvitse."
                 )).render(definition_en(
"Why are some headers red in HTML format?",
"Red properties are calculated data. They may be slow to produce and thus you might want to exclude them if you don't need them."
                 ))

                   .render(definition_fi(
"Mikä on 'duration' -parametri?",
"Joskus on absoluuttisen ajan sijaan helpompi ilmaista aikaväli suhteellisesti, esimerkiksi 'kolme päivää menneisyyteen ja viisi tulevaisuuteen'. Voit antaa tällaisia 'duration'-parametrilla ISO8601-standardin mukaisessa muodossa. Negatiivinen kesto tarkoittaa nykyhetkestä menneisyyteen, positiivinen tulevaisuuteen. Voit antaa jomman kumman tai molemmat kauttaviivalla erotettuina. Esimerkiksi 'ikuisuus' voitaisiin ilmaista '-P99Y/P99Y'. Koska 'nykyhetki' ei muodosta determinististä välimuistitettavaa URL-osoitetta, 'duration'-parametri tekee uudelleenohjauksen time-parametrilliseen osoitteeseen senhetkiseksi parhaiten tulkittavan nykyhetken perusteella."
                 )).render(definition_en(
"What is 'duration' parameter?",
"Sometimes instead of absolute time interval it's easier to express the interval relatively, for example 'three days to the past and five to the future'. You can give such intervals with 'duration'-parameter in ISO8601-standard format. Negative duration means from the present to the past, positive to the future. You can give either or both separated with a slash. For example 'eternity' could be expressed as '-P99Y/P99Y'. Because 'the present' doesn't form a deterministic cacheable URL, 'duration'-parameter makes a redirect to a time-parameterized URL based on the current best interpretation of 'the present'."

                 )).render(definition_fi(
"Rajapinta saattaa sisältää paljon laskennallista dataa, enkä saa kaikkea ladattua yhdellä pyynnöllä. Miten voin hakea dataa osissa?",
new Renderable() {
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.ol()
                .li().write("typeNames, eli alityyppiin perustuva sivutus")
                     .ul()
                         .li().write("Jos endpoint sisältää 'typeNames' -parametrin, voi siitä hakea yhden tai useamman alityypin datan kerrallaan.")._li()
                         .li().write("Hyvä ja helppo tapa, kannattaa aina hyödyntää.")._li()
                     ._ul()
                ._li()
                .li().write("tunniste, eli rivi kerrallaan")
                    .ul()
                        .li().write("Haetaan ensin pelkät tunnisteet niiltä osin mitä haluaa ladata (esimerkiksi: https://<api>/<versio>/<endpoint>.json?propertyName=tunniste?duration=-P99Y/P99Y) ja sen jälkeen varsinaisen datan tunniste kerrallaan (https://<api>/<versio>/<tunniste>.json).")._li()
                        .li().write("Pyynnöt ovat nopeita, mutta niitä joutuu tekemään mahdollisesti hyvinkin paljon.")._li()
                    ._ul()
                ._li()
                .li().write("startIndex+count, eli vakiokokoisiin joukkoihin perustuva sivutus")
                   .ul()
                       .li().write("Perinteinen tietynkokoinen sivu kerrallaan, eli valitaan sopiva 'count' ja nostetaan 'startIndex' arvoa 'count':n verran eteepäin alkaen arvosta 1 niin pitkään kunnes ei enää tule dataa.")._li()
                       .li().write("Helppo tapa, mutta jos dataan tulee muutoksia latauksen aikana, saattaa osa datasta jäädä lataamatta.")._li()
                   ._ul()
                ._li()
                .li().write("propertyName, eli sarakkeisiin perustuva sivutus")
                  .ul()
                      .li().write("Suurin osa sarakkeista on staattista sisältöä, mutta osa on laskennallista ja hyvinkin hidas muodostaa. Itselle tarpeettomat sarakkeet kannattaa jättää kokonaan pois, mutta loput voidaan ladata yksi tai useampi sarake kerrallaan. Esimerkiksi yhdellä pyynnöllä kaikki ei-laskennallinen data, ja loput niin monessa osassa kuin vaatii.")._li()
                      .li().write("Mahdollista muodostaa sopiva sivutus omaan tarpeeseen, mutta vaatii tulosten yhdistämistä latauksen jälkeen.")._li()
                  ._ul()
                ._li()
                .li().write("cql_filter, eli riveihin perustuva sivutus")
                     .ul()
                         .li().write("Jos data on jaettavissa sopivankokoisiin alijoukkoihin, voidaan 'cql_filter'-parametrilla ladata joukko kerrallaan.")._li()
                         .li().write("Joskus yhtä hyvä ja helppo kuin 'typeNames'-paramerin mukainen sivutus, mutta usein data ei jakaudu riittävän tasaisesti hyödynnettäviin joukkoihin. Lisäksi huomioitava että 'cql_filter' on tehokas vain niissä tapauksissa, kun rajaus voidaan toteutuksessa viedä tietokantakyselyyn asti, mikä ei aina ole mahdollista.")._li()
                     ._ul()
                ._li()
                .li().write("time, eli voimassaoloihin perustuva sivutus")
                    .ul()
                        .li().write("Kun data on luonteeltaan lyhytaikaista (esimerkiksi reaaliaikaiset tapahtumat), voi olla järkevää hakea ajanjakso kerrallaan. Jakson pituus on tällöin helppo valita sopivaksi pyyntöjen määrän minimoimiseksi.")._li()
                        .li().write("Ei sovellu pitkään voimassaolevaan dataan, sillä tällöin sama tietorivi palautuu useassa eri pyynnössä, jolloin myös sen laskennallinen data tulee laskettua turhaan moneen kertaan.")._li()
                    ._ul()
                ._li()
                .li().write("bbox, eli maantieteelliseen alueeseen perustuva sivutus")
                    .ul()
                        .li().write("Spatiaalista dataa voi rajata maantieteellisesti 'bbox'-parametrilla. Tämä voi olla hyvä, kun data on jakautunut maantieteellisesti riittävän tasaisesti.")._li()
                        .li().write("Huomioitava, että rajapinta ei salli mielivaltaisten bbox-rajausten antamisen, vaan rajautuu JHS 180-suosituksen mukaisiin laatikoihin.")._li()
                    ._ul()
                ._li()
            ._ol();
    }
}
                 )).render(definition_en(
"API contains lots of calculated data, and I cannot fetch everything in with a single request. How can I fetch data in smaller parts?",
new Renderable() {
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.ol()
                  .li().write("typeNames, i.e. paged by subtype")
                      .ul()
                          .li().write("If the endpoint contains 'typeNames' parameter, you can fetch data of one or more subtypes at a time.")._li()
                          .li().write("Good and easy way, should always be utilized.")._li()
                      ._ul()
                  ._li()
                  .li().write("identifier, i.e. row by row")
                      .ul()
                          .li().write("First fetch only the identifiers of the data you are interested of (for example: https://<api>/<version>/<endpoint>.json?propertyName=tunniste?duration=-P99Y/P99Y) and then the actual data one identifier at a time (https://<api>/<version>/<identifier>.json).")._li()
                          .li().write("Requests are fast, but you might need to make a lot of them.")._li()
                      ._ul()
                  ._li()
                  .li().write("startIndex+count, i.e. paged by fixed size sets")
                      .ul()
                          .li().write("Traditional fixed size page at a time, i.e. choose a suitable 'count' and raise 'startIndex' value by 'count' starting from 1 until no more data is returned.")._li()
                          .li().write("Easy way, but if there are changes in the data during the load, some of the data might be left out.")._li()
                      ._ul()
                  ._li()
                  .li().write("propertyName, i.e. paged by columns")
                      .ul()
                          .li().write("Most of the properties are static content, but some are calculated and might be slow to produce. You should always exclude the properties you don't need, but the rest can be loaded one or more columns at a time. For example all non-calculated data with one request, and the rest in as many parts as needed.")._li()
                          .li().write("Possible to form a suitable pagination for your needs, but requires combining the results after the load.")._li()
                      ._ul()
                  ._li()
                  .li().write("cql_filter, i.e. paged by rows")
                      .ul()
                          .li().write("If the data can be divided into suitable subsets, you can load them one subset at a time with 'cql_filter'-parameter.")._li()
                          .li().write("Sometimes as good and easy as 'typeNames'-parameter, but often the data doesn't divide into suitable subsets. Also note that 'cql_filter' is efficient only in cases where the filter can be pushed down to the database query, which is not always possible.")._li()
                      ._ul()
                  ._li()
                  .li().write("time, i.e. paged by validity")
                      .ul()
                          .li().write("When the data is short-lived (for example real-time events), it might be reasonable to fetch it interval by interval. The length of the interval is then easy to choose to minimize amount of requests.")._li()
                          .li().write("Not suitable for long-lived data, as then the same data row is returned in multiple requests, and thus its calculated data is calculated unnecessarily multiple times.")._li()
                      ._ul()
                  ._li()
                  .li().write("bbox, i.e. paged by geographical area")
                      .ul()
                          .li().write("Spatial data can be limited geographically with 'bbox'-parameter. This can be good when the data is divided geographically evenly enough.")._li()
                          .li().write("Note that the API doesn't allow arbitrary bounding boxes, but limits to boxes according to JHS 180-recommendation.")._li()
                      ._ul()
                  ._li()
            ._ol();
    }
}
                 ))
          ._dl()
          ._section();
    }
}
