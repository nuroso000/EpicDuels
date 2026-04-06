# Development Guide

Guide für Entwickler, die an EpicDuels arbeiten oder es erweitern möchten.

## Voraussetzungen

### Erforderlich
- **Java 21** oder höher
- **Git** zum Klonen des Repositories
- **IDE:** IntelliJ IDEA, Eclipse oder VSCode
- **Build-Tool:** Gradle oder Maven

### Optional
- **Lombok** (für Code-Generierung)
- **Docker** (für lokale Tests)

---

## Projekt klonen

```bash
git clone https://github.com/nuroso000/epicduels.git
cd epicduels
```

---

## Build Setup

### Mit Gradle (empfohlen)

#### 1. Abhängigkeiten installieren
```bash
gradle clean build
```

#### 2. JAR erstellen
```bash
gradle build
```

**Output:** `build/libs/EpicDuels-0.2.1.jar`

#### 3. Run/Test
```bash
gradle test
gradle run
```

### Mit Maven

#### 1. Abhängigkeiten installieren
```bash
mvn clean install
```

#### 2. JAR erstellen
```bash
mvn clean package
```

**Output:** `target/EpicDuels.jar`

#### 3. Run/Test
```bash
mvn test
```

---

## Projekt-Struktur

```
epicduels/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/epicduels/
│   │   │       ├── plugin/
│   │   │       ├── commands/
│   │   │       ├── listeners/
│   │   │       ├── data/
│   │   │       ├── ui/
│   │   │       ├── arena/
│   │   │       └── ...
│   │   └── resources/
│   │       ├── plugin.yml      # Plugin-Manifest
│   │       └── config.yml      # Default-Config
│   └── test/
│       └── java/
├── build.gradle                 # Gradle-Config
├── pom.xml                      # Maven-Config
├── README.md                    # Projekt-Info
├── CONTRIBUTING.md              # Beitrags-Guide
└── LICENSE.txt                  # Lizenz (CC BY-NC-SA 4.0)
```

---

## Wichtige Klassen & Module

### Main Plugin Class
```
com.epicduels.plugin.EpicDuels
```
Die Haupt-Plugin-Klasse, initialisiert alles.

### Command System
```
com.epicduels.commands.*
```
Alle Befehle sind hier implementiert.

### Event Listeners
```
com.epicduels.listeners.*
```
Minecraft-Event-Handler (Player Damage, Join, etc.).

### Data Management
```
com.epicduels.data.*
```
YAML-Datei-Management (arenas.yml, kits.yml, etc.).

### UI/GUI
```
com.epicduels.ui.*
```
Inventory-GUIs für Menüs.

### Arena System
```
com.epicduels.arena.*
```
Arena-Management und Duel-Logik.

---

## Dependencies

### Paper API
```gradle
compileOnly 'io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT'
```

Die Paper-API für Minecraft 1.21.1 (aktuellste Version).

### JUnit (für Tests)
```gradle
testImplementation 'junit:junit:4.13.2'
```

---

## Entwicklungs-Tipps

### 1. Lokaler Test-Server

Erstelle einen Test-Server:

```bash
# Paper Server JAR herunterladen
wget https://launcher.mojang.com/v1/objects/.../server.jar

# server.properties
echo "online-mode=false" >> server.properties
echo "level-type=flat" >> server.properties

# Starten
java -Xmx1G -Xms1G -jar server.jar nogui
```

### 2. Plugin debuggen

```bash
# Mit Debug-Modus starten
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar server.jar nogui

# In deiner IDE (IntelliJ/Eclipse) einen Remote Debugger connecten
```

### 3. Logs überprüfen

```bash
tail -f logs/latest.log
```

### 4. Code-Qualität

```bash
# Lint/Analysis Tools nutzen
gradle lint
gradle spotbugs
```

---

## Beitrag zum Projekt

### 1. Fork & Branch

```bash
# Fork das Projekt auf GitHub
# Clone deinen Fork
git clone https://github.com/<dein-username>/epicduels.git
cd epicduels

# Erstelle einen Feature-Branch
git checkout -b feature/my-feature
```

### 2. Implementiere die Feature

- Folge den bestehenden Code-Konventionen
- Schreibe sauberen, dokumentierten Code
- Teste lokal gründlich

### 3. Commit & Push

```bash
git add .
git commit -m "Add: New feature description"
git push origin feature/my-feature
```

### 4. Pull Request erstellen

Gehe auf GitHub und erstelle einen Pull Request mit:
- Aussagekräftiger Titel
- Detaillierte Beschreibung der Änderungen
- Tests/Überprüfungen durchgeführt
- Lizenz-Konformität (CC BY-NC-SA 4.0)

---

## Code-Konventionen

### Naming
```java
// Klassen: PascalCase
public class DuelManager { }

// Methoden: camelCase
public void startDuel() { }

// Konstanten: UPPER_SNAKE_CASE
private static final int DUEL_TIMEOUT = 300;
```

### Dokumentation
```java
/**
 * Startet ein Duel zwischen zwei Spielern.
 * 
 * @param player1 Erster Spieler
 * @param player2 Zweiter Spieler
 * @param arena Die Arena für das Duel
 * @return true wenn Duel erfolgreich gestartet
 */
public boolean startDuel(Player player1, Player player2, Arena arena) {
    // Implementation
}
```

### Null-Checks
```java
// Gut: Null-Safety
if (player != null && player.isOnline()) {
    player.sendMessage("Hello!");
}

// Mit Optional (Java 8+)
Optional.ofNullable(player)
    .filter(Player::isOnline)
    .ifPresent(p -> p.sendMessage("Hello!"));
```

---

## Testing

### Unit Tests schreiben

```java
import org.junit.Test;
import static org.junit.Assert.*;

public class DuelManagerTest {
    
    @Test
    public void testStartDuel() {
        // Arrange
        DuelManager manager = new DuelManager();
        
        // Act
        boolean result = manager.startDuel(player1, player2, arena);
        
        // Assert
        assertTrue(result);
    }
}
```

### Tests ausführen

```bash
gradle test       # Gradle
mvn test          # Maven
```

---

## Release-Process

### Version-Nummern (Semantic Versioning)

```
X.Y.Z
│ │ └─ Patch (Bug-Fixes): 0.2.0 → 0.2.1
│ └─── Minor (Features): 0.2.0 → 0.3.0
└───── Major (Breaking): 0.2.0 → 1.0.0
```

### Release erstellen

1. **Update Version** in `build.gradle` oder `pom.xml`
2. **Commit** mit Version-Tag
3. **GitHub Release** erstellen
4. **JAR** hochladen auf:
   - GitHub Releases
   - Modrinth
   - (Optional) SpigotMC, Bukkit

---

## Lizenz & Rechtliches

**EpicDuels** ist lizenziert unter **CC BY-NC-SA 4.0**.

### Bedeutung für Entwickler

- ✅ **Du kannst:** Code ändern und weiterentwickeln
- ✅ **Du darfst:** Das Projekt für dich selbst nutzen
- ❌ **Du darfst nicht:** Es kommerziell verkaufen
- ✅ **Du musst:** Die gleiche Lizenz verwenden

Siehe [LICENSE.txt](../LICENSE.txt) für Details.

---

## Weitere Ressourcen

- 📖 **Paper API Docs:** [papermc.io/docs](https://papermc.io/docs)
- 🎮 **Bukkit Plugin Dev:** [bukkit.org](https://bukkit.org)
- 🔧 **Gradle Docs:** [gradle.org](https://gradle.org)
- 📦 **Maven Docs:** [maven.apache.org](https://maven.apache.org)

---

## Probleme & Support

### Build-Fehler

```bash
# Cache löschen
gradle clean
rm -rf build/

# Neu bauen
gradle build
```

### Abhängigkeits-Probleme

```bash
# Dependencies aktualisieren
gradle dependencies
mvn dependency:tree
```

### Paper API nicht gefunden

```bash
# Repository hinzufügen (in build.gradle)
repositories {
    maven { url = "https://repo.papermc.io/repository/maven-public/" }
}
```

---

## Nächste Schritte

👉 **Erste Feature schreiben?** Siehe [CONTRIBUTING.md](../CONTRIBUTING.md)

👉 **Fragen?** Öffne ein Issue auf GitHub oder kontaktiere die Maintainer

---

**Viel Spaß beim Entwickeln! 🚀**
