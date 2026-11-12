# LiquidRatios

LiquidRatios berechnet die Mengen für Mischungen aus zwei, drei oder vier Bestandteilen, etwa für Harz und Härter. Die deutschsprachige Android-App arbeitet vollständig offline und speichert die letzte gültige Mischung lokal. Sie benötigt weder ein Konto noch eine Netzwerkberechtigung.

## Bedienung

- Wähle unter **Anzahl der Flüssigkeiten** die Zahl der Bestandteile. Tippe zum Bearbeiten auf einen Verhältniswert, eine Einzelmenge, die Gesamtmenge oder den jeweiligen Stift. **Übernehmen** oder **Fertig** bestätigt die Eingabe. **Abbrechen** verwirft den Entwurf.
- Der Slider ändert die Gesamtmenge beim Ziehen und rastet nahe 25 %, 50 %, 75 % und 100 % der Maximalmenge ein. Dazwischen betragen die Schritte 1 g bzw. 1 ml und bei einer Maximalmenge unter 1 jeweils 0,01. Fangpunkte werden auf zwei Nachkommastellen gerundet.
- Tippe rechts am Gefäß auf die Maximalmenge oder ihren Stift, um die Grenze zu bearbeiten. Bei einer Überschreitung kannst du den Eingabewert korrigieren, die Grenze manuell erhöhen oder abbrechen.
- Beim Wechsel zwischen Gramm und Milliliter bleiben die Zahlen nach Bestätigung gleich. Es findet keine physikalische Umrechnung statt.
- Die Prozentangaben am Gefäß beziehen sich auf die aktuell angezeigten Einzelmengen. Bei einer Gesamtmenge von null werden alle Bestandteile mit 0 % angegeben.
- Im Drei-Punkte-Menü kannst du alle Werte zurücksetzen und unter **Über LiquidRatios** die App-Version nachsehen. Die Startmischung ist 5:1 mit 250 g Gesamtmenge und 500 g Obergrenze.

## Starten und testen

1. Den Ordner mit `settings.gradle.kts` in Android Studio öffnen. Verwende eine Version mit Unterstützung für Android Gradle Plugin 9.4, als Gradle-JDK das eingebettete JDK oder ein kompatibles JDK ab 17.
2. Android SDK 37, Build-Tools 36.0.0 und Platform-Tools im SDK Manager bereitstellen. Der erste Gradle-Sync lädt den Wrapper und die Abhängigkeiten.
3. Im Device Manager ein Gerät ab Android 7.0 (API 24) starten. Für die visuelle Prüfung eignet sich das AVD-Profil **Samsung Galaxy A54** mit 1080 × 2340 Pixeln und 480 dpi. In der Run-Konfiguration **app** auswählen und **Run** ausführen.

Die Aufgaben lassen sich in Android Studio über Gradle oder im Projektordner über PowerShell starten:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`. Die HTML-Berichte stehen in `app/build/reports/`. Der letzte Befehl benötigt ein gestartetes Gerät oder einen Emulator. Die Unit-Tests benötigen kein Android-Gerät.

Wenn ein lokaler HTTPS-Filter bei Gradle einen Fehler wie `PKIX path building failed` auslöst, muss das verwendete Gradle-JDK der vertrauenswürdigen lokalen Stammzertifizierungsstelle vertrauen. Dafür kann eine lokale Kopie des Java-Truststores über `javax.net.ssl.trustStore` verwendet werden. Sind alle Abhängigkeiten bereits im Gradle-Cache, kann der Build mit `--offline` ausgeführt werden.

## Architektur

Das Projekt verwendet Kotlin, Jetpack Compose und Material 3, Gradle mit Kotlin DSL und Versionskatalog sowie Jetpack DataStore. Das Modul `app` gliedert sich in:

| Bereich | Aufgabe |
| --- | --- |
| `domain` | Mischungsberechnung mit `BigDecimal`, deutsches Zahlenformat und reine Zustandsübergänge |
| `data` | Atomare Speicherung der gültigen Mischung und Einstellungen in DataStore Preferences |
| `ui` | Adaptive Compose-Oberfläche, Eingabedialoge und animierte Canvas-Grafik |
| `MixViewModel` | Zentrale Zustandsquelle für die Oberfläche und sequenzielles Speichern |
| `src/test`, `src/androidTest` | JVM-Tests sowie Compose- und DataStore-Tests auf Android |

Der Zustand speichert eine Gesamtmenge oder die zuletzt ausdrücklich eingegebene Einzelmenge mit ihrer Komponente. Die Einzelmengen werden daraus abgeleitet. Ganze Hundertstel werden anhand der größten exakten Reste verteilt, bei Gleichstand in Komponentenreihenfolge. Eine direkt eingegebene Einzelmenge bleibt bei dieser Verteilung erhalten. Verhältnis- oder Komponentenanzahländerungen behalten die bisherige Gesamtmenge bei. Ausgeblendete Verhältnisse bleiben in DataStore gespeichert. Eingabeentwürfe und Dialoge gehören nur zum aktuellen UI-Zustand.

## Grenzen

- Das Gefäß stellt die Gewichts- oder Volumenanteile schematisch dar. Dichten und tatsächliche Füllstände werden nicht ermittelt.
- Manuelle Mengen haben bis zu zwölf Vor- und zwei Nachkommastellen. Verhältnisse unterstützen bis zu zwölf Vor- und zwölf Nachkommastellen. Die Übersicht zeigt maximal zwei Nachkommastellen. Sehr kleine Verhältnisse erscheinen als `<0,01`, während der Editor den vollständigen Wert zeigt.
- Auf schmalen Displays und bei größerer Schrift bricht die Kopfzeile um. Im Hochformat vergrößert freier Platz die Abstände zwischen den Bereichen. Bei wenig Platz lässt sich bis unter die Gefäßbeschriftung scrollen.

Die [Lizenzübersicht](docs/lizenzen.md) nennt die eingesetzten Drittkomponenten und die Pflichten bei einer späteren Distribution.
