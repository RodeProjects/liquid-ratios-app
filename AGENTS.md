# Projektregeln

## Versionierung

- Die App-Version steht in `app/build.gradle.kts` als `versionName` im Format `x.y.z`; der Android-`versionCode` steht daneben.
- Vor jedem Push die Version des vorgesehenen Zielbranchs prüfen. Bei einer normalen Versionsanhebung `z` genau um `1` erhöhen und `versionCode` ebenfalls genau um `1` anheben. Beide Werte gehören mit den zugehörigen Änderungen in denselben Commit.
- `x` und `y` nur auf ausdrückliche Anweisung des Benutzers ändern. Die Patchnummer läuft fortlaufend weiter; sie darf nur beim Anheben von `x` oder `y` neu beginnen.
- Prüfbranch und Übernahme desselben Commits zum Zielbranch sind eine logische Änderung. Dafür keine zweite Versionsanhebung vornehmen. Ist die lokale Version gegenüber der zuletzt auf dem Zielbranch enthaltenen Version bereits genau um eine Patchstufe erhöht, diese Version übernehmen und nicht erneut anheben.
