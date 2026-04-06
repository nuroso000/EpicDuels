# Development Guide

Guide for developers working on or extending EpicDuels.

## Prerequisites

### Required
- **Java 21** or higher
- **Git** for cloning the repository
- **IDE:** IntelliJ IDEA, Eclipse, or VSCode
- **Build Tool:** Gradle or Maven

### Optional
- **Lombok** (for code generation)
- **Docker** (for local testing)

---

## Clone the Repository

```bash
git clone https://github.com/nuroso000/epicduels.git
cd epicduels
```

---

## Build Setup

### With Gradle (Recommended)

#### 1. Install Dependencies
```bash
gradle clean build
```

#### 2. Create JAR
```bash
gradle build
```

**Output:** `build/libs/EpicDuels-0.2.1.jar`

#### 3. Run/Test
```bash
gradle test
gradle run
```

### With Maven

#### 1. Install Dependencies
```bash
mvn clean install
```

#### 2. Create JAR
```bash
mvn clean package
```

**Output:** `target/EpicDuels.jar`

#### 3. Run/Test
```bash
mvn test
```

---

## Project Structure

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
│   │       ├── plugin.yml      # Plugin manifest
│   │       └── config.yml      # Default config
│   └── test/
│       └── java/
├── build.gradle                 # Gradle config
├── pom.xml                      # Maven config
├── README.md                    # Project info
├── CONTRIBUTING.md              # Contributing guide
└── LICENSE.txt                  # License (CC BY-NC-SA 4.0)
```

---

## Key Classes & Modules

### Main Plugin Class
```
com.epicduels.plugin.EpicDuels
```
The main plugin class that initializes everything.

### Command System
```
com.epicduels.commands.*
```
All commands are implemented here.

### Event Listeners
```
com.epicduels.listeners.*
```
Minecraft event handlers (Player Damage, Join, etc.).

### Data Management
```
com.epicduels.data.*
```
YAML file management (arenas.yml, kits.yml, etc.).

### UI/GUI
```
com.epicduels.ui.*
```
Inventory GUIs for menus.

### Arena System
```
com.epicduels.arena.*
```
Arena management and duel logic.

---

## Dependencies

### Paper API
```gradle
compileOnly 'io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT'
```

Paper API for Minecraft 1.21.1 (latest version).

### JUnit (for Tests)
```gradle
testImplementation 'junit:junit:4.13.2'
```

---

## Development Tips

### 1. Local Test Server

Create a test server:

```bash
# Download Paper Server JAR
wget https://launcher.mojang.com/v1/objects/.../server.jar

# server.properties
echo "online-mode=false" >> server.properties
echo "level-type=flat" >> server.properties

# Start
java -Xmx1G -Xms1G -jar server.jar nogui
```

### 2. Debug the Plugin

```bash
# Start with debug mode
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar server.jar nogui

# Connect remote debugger in your IDE (IntelliJ/Eclipse)
```

### 3. Check Logs

```bash
tail -f logs/latest.log
```

### 4. Code Quality

```bash
# Use lint/analysis tools
gradle lint
gradle spotbugs
```

---

## Contributing to the Project

### 1. Fork & Branch

```bash
# Fork the project on GitHub
# Clone your fork
git clone https://github.com/<your-username>/epicduels.git
cd epicduels

# Create a feature branch
git checkout -b feature/my-feature
```

### 2. Implement the Feature

- Follow existing code conventions
- Write clean, documented code
- Test thoroughly locally

### 3. Commit & Push

```bash
git add .
git commit -m "Add: New feature description"
git push origin feature/my-feature
```

### 4. Create Pull Request

Go to GitHub and create a Pull Request with:
- Descriptive title
- Detailed description of changes
- Tests performed
- License compliance (CC BY-NC-SA 4.0)

---

## Code Conventions

### Naming
```java
// Classes: PascalCase
public class DuelManager { }

// Methods: camelCase
public void startDuel() { }

// Constants: UPPER_SNAKE_CASE
private static final int DUEL_TIMEOUT = 300;
```

### Documentation
```java
/**
 * Starts a duel between two players.
 * 
 * @param player1 First player
 * @param player2 Second player
 * @param arena The arena for the duel
 * @return true if duel started successfully
 */
public boolean startDuel(Player player1, Player player2, Arena arena) {
    // Implementation
}
```

### Null Checks
```java
// Good: Null safety
if (player != null && player.isOnline()) {
    player.sendMessage("Hello!");
}

// With Optional (Java 8+)
Optional.ofNullable(player)
    .filter(Player::isOnline)
    .ifPresent(p -> p.sendMessage("Hello!"));
```

---

## Testing

### Write Unit Tests

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

### Run Tests

```bash
gradle test       # Gradle
mvn test          # Maven
```

---

## Release Process

### Version Numbers (Semantic Versioning)

```
X.Y.Z
│ │ └─ Patch (Bug fixes): 0.2.0 → 0.2.1
│ └─── Minor (Features): 0.2.0 → 0.3.0
└───── Major (Breaking): 0.2.0 → 1.0.0
```

### Create Release

1. **Update version** in `build.gradle` or `pom.xml`
2. **Commit** with version tag
3. **Create GitHub Release**
4. **Upload JAR** to:
   - GitHub Releases
   - Modrinth
   - (Optional) SpigotMC, Bukkit

---

## License & Legal

**EpicDuels** is licensed under **CC BY-NC-SA 4.0**.

### For Developers

- ✅ **You can:** Modify and develop the code
- ✅ **You may:** Use the project for yourself
- ❌ **You cannot:** Sell it commercially
- ✅ **You must:** Use the same license

See [LICENSE.txt](../LICENSE.txt) for details.

---

## Additional Resources

- 📖 **Paper API Docs:** [papermc.io/docs](https://papermc.io/docs)
- 🎮 **Bukkit Plugin Dev:** [bukkit.org](https://bukkit.org)
- 🔧 **Gradle Docs:** [gradle.org](https://gradle.org)
- 📦 **Maven Docs:** [maven.apache.org](https://maven.apache.org)

---

## Issues & Support

### Build Errors

```bash
# Clear cache
gradle clean
rm -rf build/

# Rebuild
gradle build
```

### Dependency Issues

```bash
# Check dependencies
gradle dependencies
mvn dependency:tree
```

### Paper API Not Found

```bash
# Add repository (in build.gradle)
repositories {
    maven { url = "https://repo.papermc.io/repository/maven-public/" }
}
```

---

## Next Steps

👉 **Write your first feature?** See [CONTRIBUTING.md](../CONTRIBUTING.md)

👉 **Questions?** Open an issue on GitHub or contact the maintainers

---

**Have fun developing! 🚀**
