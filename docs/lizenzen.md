# Drittkomponenten und Lizenzen

Stand der Prüfung ist der 12. November 2026. Die direkten Versionen sind im [Versionskatalog](../gradle/libs.versions.toml) festgelegt. Transitive Laufzeitabhängigkeiten lassen sich mit `gradlew :app:dependencies --configuration releaseRuntimeClasspath` nachvollziehen.

| Komponente | Version | Lizenz und Quelle | Einsatz |
| --- | --- | --- | --- |
| AndroidX Activity, Lifecycle und DataStore | 1.13, 2.11 und 1.3 | [Apache 2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | Laufzeit |
| Jetpack Compose und Material 3 | BOM 2026.09.00, Material 3 1.4 | [Apache 2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | Laufzeit |
| Kotlin und Compose-Compiler | 2.4.21 | [Apache 2.0 und Hinweise](https://github.com/JetBrains/kotlin/tree/v2.4.21/license) | Laufzeit bzw. Build |
| Kotlin Coroutines, Serialization, AtomicFU | 1.9, 1.7 und 0.28 | Apache 2.0, Quellen für [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt), [Serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt) und [AtomicFU](https://github.com/Kotlin/kotlinx-atomicfu/blob/master/LICENSE.txt) | Transitive Laufzeitabhängigkeiten |
| Okio | 3.17 | [Apache 2.0](https://repo.maven.apache.org/maven2/com/squareup/okio/okio-jvm/3.17.0/okio-jvm-3.17.0.pom) | Transitive Laufzeitabhängigkeit von DataStore |
| Protobuf | in DataStore 1.3.0 umgepackt | [BSD 3-Clause](https://github.com/protocolbuffers/protobuf/blob/main/LICENSE) | Lokales Dateiformat von DataStore |
| JSpecify, JetBrains Annotations, Guava ListenableFuture | 1.0, 23.0 und 1.0 | Apache 2.0, Quellen für [JSpecify](https://github.com/jspecify/jspecify/blob/main/LICENSE), [Annotations](https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt) und [ListenableFuture](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.pom) | Transitive Laufzeitabhängigkeiten |
| Gradle und Android Gradle Plugin | 9.6 und 9.4 | Apache 2.0, Quellen für [Gradle](https://docs.gradle.org/current/userguide/licenses.html) und [Android-Tools](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.4.1/gradle-9.4.1.pom) | Build |
| Android Studio, SDK und Emulator | Android Studio mit AGP-9.4-Unterstützung, SDK 37 | [Android SDK License Agreement](https://developer.android.com/studio/terms) | Entwicklungswerkzeuge für kompatible Android-Geräte |
| JUnit und AndroidX Test | 4.13.2 und Runner 1.7 | [EPL 1.0](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt) bzw. [Apache 2.0](https://github.com/android/android-test/blob/main/LICENSE) | Tests |

Die Laufzeitbibliotheken unter Apache 2.0 und BSD 3-Clause erlauben kommerziellen, proprietären Vertrieb ohne Lizenzgebühren, Umsatzbeteiligung oder Schwellenwerte. Bei der Weitergabe müssen die Lizenztexte und erforderlichen Urheberrechts- und NOTICE-Hinweise der mitgelieferten Bibliotheken erhalten bleiben. Apache 2.0 verlangt außerdem Hinweise auf veränderte lizenzierte Dateien. BSD 3-Clause enthält eine Nichtwerbeklausel für Namen der Rechteinhaber. JUnit gehört zu den Tests. Die Pflichten der EPL gelten bei einer Weitergabe der von ihr erfassten Komponente und ihrer Änderungen. Die Android-Entwicklungswerkzeuge werden nach den SDK-Bedingungen gebührenfrei zur App-Entwicklung verwendet und gehören nicht zum Lieferumfang der App.

Gefäß, Symbole und App-Icon sind im Projekt gezeichnet. Die Oberfläche verwendet die Systemschrift von Android. Die App bündelt keine externen Bild- oder Schriftdateien.
