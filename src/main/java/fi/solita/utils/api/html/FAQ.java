package fi.solita.utils.api.html;

import static fi.solita.utils.api.html.UI.definition;
import static fi.solita.utils.api.html.UI.link;
import static fi.solita.utils.api.html.UI.text;
import static fi.solita.utils.functional.Functional.mkString;
import static org.rendersnake.HtmlAttributesFactory.href;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fi.solita.utils.api.types.Filters;

public abstract class FAQ implements Renderable {
    public static Renderable page() {
        return new Page("FAQ", new FAQ() {});
    }
    
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
          html.section()
              .dl().render(definition(
"Miksi Swagger-dokumentaatiossa on puutteita?",
"Dokumentaatio generoidaan koodista valmiilla työkalulla, joka toimii melko epävarmasti. Kuvauksen pitäisi kuitenkin olla kunnossa. Mikäli jokin swagger-kuvauksen vika ei ole jäänyt testeissämme kiinni, kerrothan meille siitä niin korjataan."
                  ))
                   .render(definition(
"Miksi Swagger-käyttöliittymä näyttää Map-paluuarvoisille metodeille tyhjän mallin ja esimerkin?",
"Tämä on bugi swagger-ui-työkalussa, ja on korjattu seuraavaan major-versioon. Itse swagger-kuvaus on kuitenkin kunnossa, joten ongelma on kosmeettinen. Asia korjaantuu kun käyttämämme kirjasto siirtyy tukemaan uudempaa swagger-ui-versiota."
                   ))
                   .render(definition(
"Miksi tunnisteet ovat sellaista muotoa kuin ovat?",
"Tunnisteen muodosta ei pidä välittää, vaan ne pitää aina käsitellä mielivaltaisina merkkijonoina. Rajapinnan palauttamat tunnisteet ovat globaalisti uniikkeja."
                   ))
                   .render(definition(
"Säilyvätkö vanhat rajapintaversiot saatavilla?",
"Kyllä. Joskus niitä kuitenkin todennäköisesti tullaan poistamaan, joten pyrithän käyttämään uusinta saatavillaolevaa versiota."
                   ))
                   .render(definition(
"Säilyvätkö vanhat rajapintaversiot muuttumattomina?",
"Rajapintalupauksena kyllä. Korjauksia niihin kuitenkin tehdään. Lisäyksiä saattaa myös tulla, esimerkiksi kokonaan uusi rajapintametodi, tai uusi vastausmuoto."
                   ))
                   .render(definition(
"Missä eri muodoissa rajapinnan data on tarjolla?",
"Swagger-dokumentaatiosta näet ajantasaisen listan. Lähes kaikki rajapintametodit on saatavilla kaikissa vastausmuodoissa."
                   ))
                   .render(definition(
"Miksi geometria on joskus vastauksessa mukana ja joskus ei?",
"Geometriat ovat joskus suuria, joten niitä ei aina laiteta oletuksena mukaan kaikissa vastausmuodoissa. Oletuksena ei-pistemäiset (eli muut kuin POINT ja MULTIPOINT) jätetään pois muista kuin spatiaalisista muodoista. Saat geometriat kuitenkin halutessasi mukaan lisäämällä geometriakentän 'propertyName'-parametriin."
                   ))
                   .render(definition(
"Miten saan piirretyä dataa kartalle?",
"Voit katsoa rajapinnan etusivun karttatoteutuksesta mallia."
                   ))
                   .render(definition(
"Etusivun karttatoteutus on melko iso, vaikka onkin melko yksinkertainen. Miten saan edes jotakin kartalle?",
link(href("minimal.html"), text("Tässä minimaalinen OpenLayers 3 -esimerkki."))
                   ))
                   .render(definition(
"Mikä on time-paremetri?",
"Parametri määrittää ajanhetken tai aikavälin, jota voimassaololtaan leikkaavat rivit palautetaan. Yleisin tarve on 'nykyhetki', joksi suosittelemme käytettävän aina edellistä UTC-ajan keskiyötä (T00:00:00Z) välimuistiosuvuuden parantamiseksi."
                   ))
                   .render(definition(
"Miksi aikaleimoissa on joskus perässä Z ja joskus ei, mitä aikoja nämä ovat?",
"Yhtenäisyyden ja selkeyden nimissä rajapinta palauttaa aikaleimat aina UTC-ajassa, ja jättää aikavyöhykemuunnokset käyttäjän harteille. Ihmisen luettavaksi tarkoitetuissa vastausmuodoissa (HTML, CSV, XLSX) kuitenkin palautetaan aikaleimat Suomen aikavyöhykkeessä sekaannusten välttämiseksi."
                   ))
                   .render(definition(
"Pitääkö minun rajoittaa propertyName-parametrilla data aina mahdollisimman pieneksi?",
"Se riippuu. Osa datasta on raskaampi hakea tai laskennallista, joten sinänsä kannattaa pyytää vain se mitä tarvitsee. Toisaalta, välimuistiosuvuuden parantamiseksi kannattaa varioida parametreja mahdollisimman vähän. Jos haluat lähes kaiken datan, kannattaa todennäköisesti pyytää kaikki."
                   ))
                   .render(definition(
"Mitä arvoja propertyName-parametri hyväksyy?",
"Karkeastiottaen ne, jotka näkyvät kyseisen rajapintametodin HTML-muodon sarakeotsikkoina."
                   ))
                   .render(definition(
"Useimmissa vastauksissa sallitaan ikuinen välimuistitus, voinko luottaa tähän?",
"Kyllä, cache-control-headereihin voi luottaa, ja voit näinollen halutessasi laittaa kutsujen väliin oman välimuistipalvelimen mikäli koet sen hyödylliseksi käyttötapaukseesi."
                   ))
                   .render(definition(
"Miksi rajapinta tekee paljon redirectejä?",
"Välimuistituksen parantamiseksi. Tekemällä uudelleenohjauksia voidaan varsinaiset dataa palauttavat vastaukset välimuistittaa useimmiten ikuisesti."
                   ))
                   .render(definition(
"Mikä on urlin alkuun ilmestyvä numerosarja?",
"Se kuvaa tietokannan jonkinlaista versionumeroa. Mikäli numero ei muutu, ei datakaan ole päivittynyt tietokannassa. Tätä numeroa ei kuulu liittää mukaan varsinaisiin kutsuttaviin rajapintametodeihin, vaan niihin siirrytään redirectin kautta. Paitsi jos haluat optimoida, ja hoidat datan päivittymisen tarkastelut manuaalisesti."
                   ))
                   .render(definition(
"Näenkö bi-temporaalisuuden toista historiaa (transaktioaika) jos muutan url:n alkuun ilmestyvää numeroa?",
"Vain niiltä osin mitä sattuu säilymään jossakin http-välimuistissa. Vaikka rajapinta kertookin historian (validisuusaika), ei se ole bi-temporaalinen."
                   ))
                   .render(definition(
"Miksi monet json-vastaukset tulevat objektina eivätkä taulukkona?",
"Kyseltäessä yksittäisellä ajanhetkellä, sisältävät avaimen alla olevat taulukot aina vain yhden rivin, joka kuvaa miltä käsite näyttää kyseisellä ajanhetkellä. Kyseltäessä aikavälillä, voi tuo taulukko sisältää useita rivejä mikäli käsite muuttuu jotenkin pyydetyllä aikavälillä. Objekti-rakenne takaa, että yhteen käsitteeseen liittyvät rivit löytyvät aina vastaavan avaimen alta, eikä sinun tarvitse varautua siihen että taulukossa olisi joskus useampia saman käsitteen rivejä. Mikäli koet että tämä aiheuttaa ongelmia esimerkiksi tiettyjen työkalujen toimivuuden suhteen, ota yhteyttä."
                   ))
                   .render(definition(
"Miksi pyynnöissä on niin tiukat vaatimukset, kuten parametrien aakkosjärjestys?",
"Välimuistiosuvuuden parantamiseksi. Mikäli koet että tämä aiheuttaa ongelmia esimerkiksi tiettyjen työkalujen toimivuuden suhteen, ota yhteyttä."
                   ))
                   .render(definition(
"Missä koordinaattijärjestelmässä geometriat ovat?",
"ETRS-TM35FIN-karttaprojektio ja -tasokoordinaatisto (EPSG:3067)."
                   ))
                   .render(definition(
"Vektorit vai kuvat?",
"Oma valintasi. Kuvat ovat suorituskykyisempiä erityisesti mobiililaitteissa, mutta vektorien kanssa voi vuorovaikuttaa. Yksi mahdollisuus on ladata 'korkeilla' zoom-tasoilla tasot kuvina ja vaihtaa vektoreihin kun lähestytään zoom-tasoa, jolla käyttäjän voisi olettaa haluavan vuorovaikuttaa kyseisen tason kanssa."
                   ))
                   .render(definition(
"Mitä cql_filter-parametri tekee?",
"Suodattaa tulosjoukkoa ehtojen perusteella. Useita ehtoja voi yhdistää AND-sanalla. Osajoukko ECQL-filttereistä: " + mkString(", ", Filters.SUPPORTED_OPERATIONS) + ". INTERSECTS vaatii ensimmäiseksi parametriksi geometria-propertyn ja toiseksi POLYGON-tyyppisen WKT-geometrian."
                  ))
                   .render(definition(
"Mistä saan WKT-muotoisia polygoneja?",
"Voit piirtää etusivun kartalle polygonin, ja saat sen WKT-muodossa."
                  ))
                   .render(definition(
"Miten piirrän infran kaaviomuodossa / mitä ovat kaaviogeometriat?",
"Käsitteillä on todellisten koordinaattisijaintien lisäksi \"kaaviokoordinaattisijainnit\", joita käyttämällä infra voidaan piirtää kaaviomaisena. Esitystapa voidaan valita \"presentation\" parametrilla."
                  ))
              ._dl()
          ._section();
    }
}
