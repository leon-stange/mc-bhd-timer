# BHD - Timer

**BHD - Timer** ist ein eigenes Minecraft-Server-Plugin für Minecraft 26.3 mit
PaperMC.

Die erste Plugin-Version ist **v0.0.1**.

Der technische Plugin-Name und Datenordner lauten `BHD-Timer`; als Log-Präfix wird
`BHD - Timer` verwendet.

Das Plugin stellt einen gemeinsamen, aufwärtszählenden Timer für den Server
bereit. Alle Spieler sehen denselben Zeitstand und denselben Sichtbarkeitsstatus.

## Befehle

| Befehl | Funktion |
| --- | --- |
| `/bhd-timer start` | Den Timer bei `0` starten oder vom pausierten Zeitstand aus fortsetzen. |
| `/bhd-timer stop` | Den Timer pausieren und den aktuellen Zeitstand behalten. |
| `/bhd-timer reset` | Das Zurücksetzen anfordern und durch erneute Eingabe innerhalb von fünf Sekunden bestätigen. Erst nach der Bestätigung wird der Timer angehalten und auf `0h 0m 0s` zurückgesetzt. Ein anschließendes `start` beginnt bei `0`. |
| `/bhd-timer hide` | Die Timer-Anzeige für alle Spieler ausblenden, ohne den Laufstatus zu ändern. Ein laufender Timer zählt im Hintergrund weiter. |
| `/bhd-timer show` | Die Timer-Anzeige für alle Spieler mit dem aktuellen Zeitstand wieder einblenden, ohne den Laufstatus zu ändern. |
| `/bhd-timer version` | Die aktuell geladene Plugin-Version im Chat anzeigen, beispielsweise `BHD-Timer: Version v0.0.1`. |

Der Hauptbefehl lautet genau `/bhd-timer`, der Pause-Unterbefehl genau `stop`.
Wiederholtes `start` bei bereits laufendem Timer setzt den Zeitstand nicht zurück
und startet keinen zusätzlichen Timer.

## Reset-Bestätigung

- Der erste Aufruf von `/bhd-timer reset` fordert nur eine Bestätigung an.
  Zeitstand und Laufstatus bleiben dabei unverändert.
- Der ausführende Operator erhält im Chat einen Hinweis, dass er den Befehl
  innerhalb von fünf Sekunden erneut eingeben muss, um den Reset zu bestätigen.
- Nur die erneute Eingabe durch denselben Operator innerhalb dieses Zeitfensters
  bestätigt den Reset. Der Timer wird dann auf `0h 0m 0s` gesetzt und pausiert.
- Verstreichen die fünf Sekunden ohne Bestätigung, verfällt die Anfrage und der
  Timer bleibt unverändert. Eine spätere Eingabe startet eine neue Anfrage.
- Die Sichtbarkeit bleibt beim Reset unverändert.
- `start`, `stop` und ein erfolgreicher Reset verwerfen alle ausstehenden
  Bestätigungen. Beim Verlassen des Servers verfällt die eigene Bestätigung.

## Berechtigungen

Nur Server-Operatoren dürfen die Befehle ausführen. Spieler ohne Operator-Status
erhalten bei einem Aufruf exakt folgende Fehlermeldung im Chat:

```text
BHD-Timer: keine Berechtigung.
```

Ein abgelehnter Aufruf verändert weder den Timer noch seine Sichtbarkeit.

## Timer-Anzeige im Spiel

Der Timer soll direkt im Spiel angezeigt und über die oben genannten Befehle
gesteuert werden. Er zählt die verstrichene Zeit aufwärts.

- Darstellung während des Laufens nach dem bereitgestellten Referenzbild:
  zentrierter, hellblauer
  beziehungsweise türkiser, fett dargestellter Text mit Minecraft-Textschatten.
- Zeitformat mit Stunden, Minuten und Sekunden, beispielsweise `3h 3m 24s`.
- Die laufende Anzeige wird sekündlich aktualisiert.
- Beim Stoppen bleibt die erreichte Zeit sichtbar, gefolgt von ` - Pausiert`,
  beispielsweise `3h 3m 24s - Pausiert`. Die gesamte Pausenanzeige einschließlich
  Zeitstand und Zusatz erscheint hellrot und kursiv.
- Beim Fortsetzen zählt der Timer von diesem Stand aus weiter. Der Zusatz
  ` - Pausiert` entfällt und die Anzeige verwendet wieder die Darstellung für
  den laufenden Timer.
- Nach einem bestätigten Reset zeigt die Anzeige `0h 0m 0s - Pausiert`, sofern
  sie eingeblendet ist. Der Timer bleibt bis zum nächsten `start` angehalten.
- Die Anzeige verwendet vorerst die normale Actionbar-Position unten mittig.
  Eine eigene vertikale Positionierung ist nicht vorgesehen.
- `hide` und `show` wirken serverweit und sind für alle Spieler synchronisiert.
  Neu beitretende Spieler übernehmen ebenfalls die aktuelle Sichtbarkeit und
  sehen beim eingeblendeten Timer den gemeinsamen Zeitstand.
- Das Ausblenden beeinflusst die Zeitmessung nicht. Ein pausierter Timer bleibt
  beim Aus- oder Einblenden pausiert; ein laufender Timer läuft weiter.

Die Anzeige lässt sich mit der
[Actionbar von Paper/Adventure](https://jd.papermc.io/paper/26.3/org/bukkit/entity/Player.html)
umsetzen, ohne dass Spieler einen zusätzlichen Mod oder ein eigenes Resourcepack
installieren müssen.

## Speicherung über Serverneustarts hinweg

Zeitstand und gemeinsame Sichtbarkeit werden dauerhaft in
`plugins/BHD-Timer/state.yml` gespeichert. Auf unserem Server liegt
diese Datei damit unter
`/home/leon/docker-volumes/minecraft-server/plugins/BHD-Timer/state.yml` und bleibt
durch das eingebundene Datenverzeichnis auch bei einem Containerwechsel erhalten.

- Der Zeitstand wird alle fünf Sekunden während des Betriebs und beim geordneten
  Beenden des Servers gespeichert. Die Dateizugriffe erfolgen zum Speichern auf
  einem separaten Thread. Eine fertige temporäre Datei ersetzt die bisherige
  Statusdatei, damit sie beim Schreiben nicht teilweise überschrieben wird.
- Auch Änderungen durch `stop` und einen bestätigten `reset` werden gespeichert,
  damit ein alter Zeitstand nach einem Neustart nicht wiederhergestellt wird.
- Beim Serverstart wird der gespeicherte Zeitstand wieder eingelesen. Ein
  Serverneustart darf den Timer nicht unbeabsichtigt auf `0` zurücksetzen.
- Nach einem Serverneustart bleibt der Timer immer pausiert, auch wenn er vor
  dem Neustart lief. Ein Operator muss ihn mit `/bhd-timer start` ausdrücklich
  fortsetzen. Bei eingeblendeter Anzeige erscheint der gespeicherte Zeitstand
  mit dem Zusatz ` - Pausiert` in hellrot und kursiv.
- Die Zeit, in der der Server ausgeschaltet ist, wird nicht mitgezählt.
- Die Sichtbarkeit wird ebenfalls wiederhergestellt: Ein vor dem Neustart
  ausgeblendeter Timer bleibt ausgeblendet.
- Ist die Statusdatei ungültig, deaktiviert sich das Plugin mit einer Meldung im
  Server-Log. Die vorhandene Datei wird dabei nicht mit einem leeren Zeitstand
  überschrieben.

## Bauen

Voraussetzung ist ein **JDK 25**. Weitere Programme oder Plugins sind nicht nötig;
der Gradle-Wrapper lädt das Build-Werkzeug und die Abhängigkeiten beim ersten
Aufruf herunter.

```bash
./gradlew build
```

Unter Windows kann stattdessen `gradlew.bat build` verwendet werden.
Die fertige Datei liegt unter `build/libs/BHD-Timer-0.0.1.jar`.
Die Tests für Zeitmessung, Reset-Bestätigung, Anzeige und Speicherung laufen beim
Build mit. Paper und die Testbibliotheken werden nicht in die Plugin-JAR gepackt.

## Installieren

1. `build/libs/BHD-Timer-0.0.1.jar` in den `plugins`-Ordner des Paper-Servers
   kopieren. Auf unserem Server ist das
   `/home/leon/docker-volumes/minecraft-server/plugins/`.
2. Den Minecraft-Server vollständig neu starten:

   ```bash
   sudo docker restart --time 90 minecraft-server
   ```

3. Als Operator mit `/bhd-timer start` starten. Der Timer ist beim ersten Start
   sichtbar und bei `0h 0m 0s` pausiert.

Es sind keine Datenbank, keine zusätzlichen Server-Plugins, kein Client-Mod und
kein eigenes Resourcepack erforderlich. Die Zeitmessung verwendet eine monotone
Uhr statt Server-Ticks; bei Lag wird die tatsächlich verstrichene Laufzeit gezählt.
Bei einem harten Absturz kann höchstens der noch nicht gespeicherte Abschnitt
seit der letzten erfolgreichen Sicherung fehlen.

## Aufbau

- `src/main/java/`: Plugin-Einstieg, Timer, Reset-Bestätigung und Statusdatei.
- `src/main/resources/plugin.yml`: Plugin- und Befehlsregistrierung für Paper 26.3.
- `src/test/java/`: automatisierte Verhaltenstests.
- `build.gradle.kts`, `settings.gradle.kts` und Gradle-Wrapper: Build mit Java 25.

Grundlagen: [Paper-Projektaufbau](https://docs.papermc.io/paper/dev/project-setup/)
und [plugin.yml](https://docs.papermc.io/paper/dev/plugin-yml/).
