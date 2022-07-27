# GeoContentProvider-Android
An Android Content Provider for geodata which can be used when mobile devices are offline or when multiple apps are useing the same geodata

[English is following soon]

# Content Provider

Die .apk ist die Provider-App mit Daten von Berlin.

Eine Client-App kann Anfragen an die App schicken, um dann Geodaten zu erhalten. Derzeit unterstützt:

* Get-Feature von öffentlichen Daten aus Berlin (nodes und ways)
* Get-Feature mit privaten Daten von Client-Apps (siehe Update-Feature)
* Update Feature mit privaten Client-Daten
* User Location History (Datenbank mit Positionen des Users alle 20 Minuten)
* Geocoding-Abfrage mit Koordinaten als Input und Ortsnamen als Output


## Get Feature

*Veränderungen seit der letzten Version (14.07.2022):*

* *Veränderungen von Parameter-Namen:*
  * *Das Ergebnis wird im Bundle mit dem Key "response" versehen, nicht mehr mit "result"*
  * *Mit typename public und private bestimmen, welche Daten angefragt werden sollen*
* *Neue Content-Uri: content://org.geoit.geocontentprovider, nicht mehr de.geocontentprovider.provider*
* *Statusabfrage des Responses: unter bundle.getInt("status") erhält man den Statuscode nach HTTP-Standard.*
  * *200 = Okay*
  * *404 = Client hat falsche Angaben an den Provider geschickt*
  * *500 = Irgendetwas ist schief gelaufen, doch es ist nicht klar, was. Probiere es noch einmal*
* *POST-Request möglich. Schicke ein JSON Objekt (OSM like: Nodes, Ways, Relations mit Referenzen) an den Provider, der dies unter einer ID speichert. Anschließend kannst du die Daten unter Angabe der ID wieder abfragen (mehr dazu unten)*

Letztes mal hatte ich einen Cursor gezeigt. Doch es kam die Frage auf, wie der funktioniert und ob es auch möglich ist, ein JSON auszugeben. Deshalb hat sich die Schnittstelle etwas geändert (und wird sich in Zukunft vielleicht noch ändern, je nachdem was sonst noch gefordert wird).

Um eine Anfrage an den Content Provider zu schicken, muss eine call-Methode aufgerufen werden. Diese ist in der Content Provider Class implementiert und hilft insofern, dass man nicht wie bei der query-Methode streng einen Cursor zurückbekommt, sondern alles mögliche. Der Einfachheit halber wird das Ergebnis immer als String zurückgegeben.

So sieht der Code aus, um eine Anfrage an den Provider zu schicken:

```
Bundle bundle = new Bundle();
bundle.putString("request", "getFeature");
bundle.putString("typename", "public");
bundle.putString("type", "way");
bundle.putString("name", "Alexanderplatz");
bundle.putDouble("minLat", 52.511896);
bundle.putDouble("minLon", 13.390900);
bundle.putDouble("maxLat", 52.527381);
bundle.putDouble("maxLon", 13.420148);

Bundle query = getContentResolver().call(CONTENT_URI, "", null, bundle);
Log.d("mLog", query.getString("response"));
```

Ein Bundle ist eine Java-Klasse, mit der Daten transportiert werden können. Dies sind einfache key-value-Paare, wie man es von OSM auch kennt. Wir übergeben in dem Beispiel die Grenzen der Bounding Box. Dazu müssen die Keys unbedingt minLat, minLon, maxLat und maxLon heißen. Als 2. Parameter kommt euer Wert hin. Außerdem übergibt ihr einen Double-Wert, deshalb put**Double**. Dann muss noch ein type übergeben werden. Das kann ein node oder way sein im String-Format. Dann braucht das bundle noch einen servicetype, der bei der Feature-Abfrage "getFeature" heißt. Anschließend könnt ihr ein osm-key-value-Paar übergeben. In dem Beispiel übergeben wir den key "name" mit dem value "Alexanderplatz".

Anschließend wird der Provider angesprochen mit:

```
getContentResolver().call(CONTENT_URI, "", null, bundle);
```

Hierbei übergeben wir das vorher erstellt Bundle als letzten Parameter. Der erste Parameter ist die Uri, die da heißt:

```
private static final Uri CONTENT_URI = Uri.parse("content://org.geoit.geocontentprovider");
```

Der 2. und 3. Parameter bleiben noch leer. Anpassungen an der Schnittstelle sind jederzeit möglich.

Das Ergebnis speichert ihr als Bundle ab. Das Ergebnis erhaltet ihr mit : bundle.getString("response"); und könnt es in einer String-Variable abspeichern.

Wichtige Ergänzung: Im Manifest muss ab Android 11 deklariert werden, dass ihr einen Content Provider anspricht. Außerhalb des <application> tags muss stehen:

```
<queries>
    <package android:name="org.geoit.geocontentprovider" />
</queries> 
```

##### Format

Es werden Formate zur Ausgabe unterstützt:

* json: OSM-like. Es gibt Nodes, Ways und Relations, die Referenzen aufeinander haben. Lediglich Nodes besitzen Koordinaten
* osm: OSM-like in XML-Form. Struktur wie bei JSON
* geojson: Feature-orientiert. Koordinaten stehen direkt in den Objekten.

##### Type

* node
* way
* ~~relation~~ *derzeit nicht unterstützt*

##### Request & Typename

* getFeature: Anfrage, um Geodaten im spezifizierten Format zu erhalten
  * typename = public: Es wird in den öffentlichen Daten geschaut (derzeit Berlin)
  * typename = private: Apps können mit updateFeature Daten an den Provider senden. Der Provider gibt dann diese Daten mit entsprechender ID wieder zurück. Kann als externer Speicher einer App genutzt werden (mehr dazu unter *Update Feature*)
* updateFeature: Daten können wie POST als Bundle an den Provider geschickt werden, der die Daten wie eine externer Speicher verwaltet und ausgibt (mehr dazu unten)
* getUserLocationHistory: Die App nimmt den Standort alle 30 Sekunden und speichert sie in einer für alle Apps öffentlichen Datenbank ab (mehr dazu unten)

##### Ergebnis

Im Bundle wird ein Key "response" mitgegeben werden, in dem das Ergebnis im spezifizierten Format als String-Objekt steckt. Falls ein Fehler unterlaufen ist, wird dieser unter "response" ausgegeben. Außerdem kann der Statuscode abgefragt werden, ähnlich dem HTTP-Standard.

* 200 = Okay
* 404 = Client hat falsche Angaben an den Provider geschickt
* 500 = Irgendetwas ist schief gelaufen, doch es ist nicht klar, was. Probiere es noch einmal

## Update Feature

Es ist möglich, Daten an den Provider zu senden, der diese verwaltet und auf Nachfrage ausgibt. Im Prinzip ist es ein externer Speicher. Die Client-App muss sich also keine Gedanken um die Speicherung machen, sondern schickt einfach Daten in einem Format (derzeit nur json, OSM-like) an den Provider.

```
bundle.putString("content", someJsonObject);
bundle.putString("request", "updateFeature");
bundle.putString("typename", "private");
bundle.putString("id", "clientID1");
bundle = getContentResolver().call(CONTENT_URI, "", null, bundle);
bundle.getString("response");
```

Der Request ist updateFeature, der typename private, da private Daten einer App transportiert werden. Unter "content" wir dein String-Objekt im JSON-Format (OSM-like) mitgegeben. Das sind die Daten, die gespeichert werden sollen. Es muss außerdem eine ID vergeben werden. Die kann beliebig sein, jedoch muss sie vom Client gemerkt werden, da sie benötigt wird, wenn diese Daten wieder abgefragt werden. Eine eventuelle Fehlermeldung oder Bestätigung wird mit "response" mitgeliefert.

Der Content Provider nimmt die Daten und schreibt sie eine interne PBF-Datei. Deswegen geschieht das Auslesen dieser Daten anschließend gleich wie die normale getFeature-Anfrage von öffentlichen Daten.

```
bundle.putString("request", "getFeature");
bundle.putString("typename", "private");
bundle.putString("id", "clientID1"); // ask for data with indiv. id
bundle.putDouble("minLat", 52.511896);
bundle.putDouble("minLon", 13.390900);
bundle.putDouble("maxLat", 52.527381);
bundle.putDouble("maxLon", 13.420148);
bundle.putString("type", "way");
bundle.putString("name", "Alexanderplatz");
bundle = getContentResolver().call(CONTENT_URI, "", null, bundle);
bundle.getString("response");
```

Lediglich muss typename auf "private" gestellt und dieselbe ID mitgegeben werden. Es ist möglich, von den gespeicherten Daten nur ein Teil abzufragen, je nachdem, welche keys und values mitgegeben werden. Das Ergebnis findet sich unter "response". Möglicherweise funktioniert das Einlesen nicht korrekt, dann empfehle ich, zunächst dieselbe Anfrage noch einmal zu stellen. Ansonsten sollten die Parameter überprüft werden. Auch hier lassen sich die Statuscodes anzeigen.

## User Location History

Die Provider App muss einmal vom user geöffnet werden und dann auf den Button "Standortverlauf beginnen" klicken. Dann wird alle 20 Minuten der Standort bestimmt und in einer Datenbank gespeichert.

```
Bundle bundle = new Bundle();
bundle.putString("afterDate", "2022-06-28");
bundle.putString("afterTime", "07:58:00");
bundle.putString("beforeDate", "2022-06-28");
bundle.putString("beforeTime", "07:59:00");
bundle.putString("servicetype", "getUserLocationHistory");
Bundle queryOSM = getContentResolver().call(CONTENT_URI, "", null, bundle);
log(queryOSM.getString("result"));
```

Hier wird wieder ein Bundle übergeben. Es ist dasselbe Prinzip wie bei der Feature-Abfrage. Man übergibt die Key-Value-Paare:

* afterDate: alles nach diesem Datum
* afterTime: alles nach dieser Zeit
* beforeDate: alles vor diesem Datum
* beforeTime: alles vor dieser Zeit

Alle Daten werden als String wie in dem Beispiel übergeben. Die Daten können auch null sein. Das heißt, wenn ihr grundsätzlich alle Standorte haben wollt, müsst ihr gar kein Date- oder Time Argument übergeben. Der servicetype muss aber immer übergeben werden, diesmal mit "getUserLocationHistory". Das Ergebnis ist ein String im Json-Format.

Beachtet: Wenn ihr einen virtuellen Emulator nutzt, müsst ihr sicherstellen, dass dieser Emulator Google Play Services nutzt. Sonst erhaltet ihr eine Fehlermeldung, dass die App keinen Zugriff auf Google Play hat. Dann müsst ihr einen neuen Emulator erstellen, der das Play-Zeichen besitzt.

Es ist auch möglich, eine Geocoding-Abfrage zu erstellen (siehe Bachelorarbeit).
