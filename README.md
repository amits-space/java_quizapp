# 🧠 MindGlow Quiz Portal

> A full-stack interactive quiz web app built with Java Servlets, MySQL, and a custom Pastel UI — deployed on Apache Tomcat.

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql&logoColor=white)
![Tomcat](https://img.shields.io/badge/Apache%20Tomcat-10.x-yellow?logo=apachetomcat&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-3.9-red?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

---

<!-- Add a screenshot or screen recording of the app here -->
<!-- Example: ![MindGlow Demo](docs/demo.gif) -->

---

## What is this?

MindGlow is a browser-based quiz game built entirely from scratch as a **BIS402 course project**. Pick a category, choose your game mode, and race against the clock or take it slow — your scores are saved to a live leaderboard.

No frameworks. No Spring. Just pure Java Servlets, raw JDBC, and a hand-crafted Single Page Application frontend — because sometimes the best way to understand how the web works is to build it without shortcuts.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Game Modes](#game-modes)
- [Categories](#categories)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Adding Questions](#adding-questions)
- [Troubleshooting](#troubleshooting)

---

## Features

- 🎮 **Two game modes** — relaxed MCQ (Classic) and a timed free-text blitz (Rapid Fire)
- 🏆 **Global leaderboard** with Gold, Silver, and Bronze trophies for the top 3
- 🌙 **Light / Dark theme** toggle with smooth, hardware-accelerated transitions
- 🗄️ **Zero-touch database setup** — on first boot, the app creates the `quizdb` schema and seeds all 120+ questions automatically via a `ServletContextListener`
- 🔒 **Server-side validation** — scores and the Rapid Fire countdown are tracked in the Java `HttpSession`, not the browser, so DevTools cheating is blocked
- 🔀 **Randomised questions** every round via `Collections.shuffle()` — no two sessions are the same
- 🎊 **Confetti celebration** on game end
- 🔄 **Refresh-safe sessions** — accidentally closing the tab mid-game? Your score and position are preserved

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java Servlets (Jakarta EE 10) |
| Server | Apache Tomcat 10 |
| Database | MySQL 8.x via JDBC |
| Connection Pooling | HikariCP |
| Build Tool | Apache Maven 3.9 (bundled — no install needed) |
| Frontend | Vanilla HTML5 + CSS3 + JavaScript (SPA) |
| Data Seeding | JSON + `ServletContextListener` |

---

## Game Modes

### Classic Mode *(Relaxed)*
10 randomised multiple-choice questions, no time limit.
- ✅ Correct answer → **+4 points**
- ❌ Wrong answer → **-1 point**
- ⏭️ Pass / Skip → **0 points** (no penalty)
- Final score is revealed after all 10 questions via a Submit button.

### Rapid Fire *(Intense)*
Unlimited questions in a strict **90-second countdown**. Type your answer — matching is case-insensitive.
- ✅ Correct answer → **+4 points**
- ❌ Wrong answer → **-1 point**
- ⚠️ Skip → **-1 point** (a warning dialog appears before deducting)
- Quitting early still saves whatever score you've accumulated.

---

## Categories

| Category | Coverage |
|---|---|
| 🎬 Bollywood | Directors, iconic films, dialogues, award winners, box-office hits |
| 🎥 Hollywood | MCU, Harry Potter, LOTR, DC, and other globally popular franchises |
| 🎵 Bollywood Music | Mainstream playback songs from the 1990s to the 2020s |
| ✈️ Aviation | Aircraft, airports, aerodynamics, history, ICAO codes |
| 💻 Technology | Programming, the internet, hardware, AI, and tech history |
| 🎮 Gaming | GTA, RDR, BGMI, Witcher, God of War, and other major titles |

Each category has **20–25 questions** in the database.

---

## Getting Started

### Prerequisites

Before you begin, make sure you have the following installed:

- **JDK 11 or higher** — [Download from Adoptium](https://adoptium.net)
- **MySQL 8.x** running on `localhost:3306` — [Download MySQL](https://dev.mysql.com/downloads/installer/)
- **Apache Tomcat 10.x** extracted to a folder with no spaces in the path (e.g. `C:\tomcat10`) — [Download Tomcat](https://tomcat.apache.org/download-10.cgi)

> You do **not** need to install Maven — it is bundled inside `.maven/apache-maven-3.9.16/`.

---

### 1. Clone the repository

```bash
git clone https://github.com/amits-space/java_quizapp.git
cd java_quizapp
```

---

### 2. Configure database credentials

Open `src/main/resources/config.properties` and update the credentials to match your MySQL setup:

```properties
db.url=jdbc:mysql://localhost:3306/quizdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
db.username=root
db.password=
```

> Leave `db.password=` empty if you installed MySQL without a root password. You do **not** need to create the `quizdb` database manually — the app does it on first startup.

---

### 3. Build the WAR file

**Windows (PowerShell):**
```powershell
.\.maven\apache-maven-3.9.16\bin\mvn clean package
```

**Linux / macOS:**
```bash
./.maven/apache-maven-3.9.16/bin/mvn clean package
```

On success, you will see `BUILD SUCCESS` and the file `target/quizapp.war` will be created.

---

### 4. Deploy to Tomcat

**Windows:**
```powershell
copy target\quizapp.war C:\tomcat10\webapps\
C:\tomcat10\bin\startup.bat
```

**Linux / macOS:**
```bash
cp target/quizapp.war /opt/tomcat10/webapps/
/opt/tomcat10/bin/startup.sh
```

---

### 5. Open in your browser

```
http://localhost:8080/quizapp/
```

On first startup, Tomcat will automatically create the `quizdb` database and seed all 120+ questions. No SQL imports required.

---

## Project Structure

```
java_quizapp/
├── .maven/apache-maven-3.9.16/     ← Bundled Maven (no install needed)
├── pom.xml                          ← Dependencies & build config
├── seed.sql                         ← Manual question seed (optional fallback)
└── src/main/
    ├── java/com/quizapp/
    │   ├── dao/          ← Database access objects (QuestionDAO, UserScoreDAO)
    │   ├── model/        ← POJOs (Question, UserScore, GameSession)
    │   ├── servlet/      ← HTTP handlers (NameEntry, StartGame, Answer, Score...)
    │   └── util/         ← ConfigLoader, JsonUtils
    ├── resources/
    │   ├── config.properties        ← DB connection config
    │   └── data/questions/
    │       └── questions.json       ← 120+ question seed bank
    └── webapp/
        ├── index.html               ← Single Page Application shell
        ├── css/style.css            ← Pastel design system (light + dark tokens)
        ├── js/app.js                ← SPA router + confetti engine
        └── WEB-INF/web.xml          ← Servlet URL mappings
```

---

## Adding Questions

Questions live in two places. Use whichever suits your workflow:

### Option A — Edit `seed.sql` (quick, no rebuild needed)

Add new `INSERT` statements to `seed.sql` and run it directly against MySQL:

```sql
INSERT INTO questions (category, question_text, answer_text, options_json, difficulty, tags)
VALUES (
  'Gaming',
  'Which studio developed Red Dead Redemption 2?',
  'Rockstar Games',
  '["Rockstar Games","Naughty Dog","Ubisoft"]',
  'easy',
  'rdr,rockstar'
);
```

```bash
mysql -u root -p < seed.sql
```

> ⚠️ `seed.sql` starts with `TRUNCATE TABLE questions;` — remove that line if you don't want to wipe existing data.

### Option B — Edit `questions.json` (permanent, baked into the build)

Add entries to `src/main/resources/data/questions/questions.json`, then drop the database and rebuild so the `ServletContextListener` re-seeds everything fresh:

```json
{
  "category": "Gaming",
  "questionText": "Which studio developed Red Dead Redemption 2?",
  "answerText": "Rockstar Games",
  "optionsJson": ["Rockstar Games", "Naughty Dog", "Ubisoft"],
  "difficulty": "easy",
  "tags": "rdr,rockstar"
}
```

**Valid category names (case-sensitive):** `Bollywood`, `Hollywood`, `Aviation`, `Bollywood Music`, `Technology`, `Gaming`

**Valid difficulty values:** `easy`, `medium`, `hard`

---

## Troubleshooting

### "No questions found for category" error after restarting Tomcat

This happens when MySQL stops running between sessions. The app's auto-seeder only triggers when the database doesn't exist yet — once the tables are there, restarting Tomcat alone won't re-seed them.

**Fix:**

1. Make sure MySQL is running:
   ```powershell
   # Windows
   net start MySQL80
   ```
   ```bash
   # Linux/macOS
   sudo systemctl start mysql
   ```

2. Drop the old database so the seeder runs fresh on next startup:
   ```sql
   mysql -u root -p
   > DROP DATABASE IF EXISTS quizdb;
   > exit;
   ```

3. Restart Tomcat — the database will be recreated and re-seeded automatically.

> **Pro tip for Windows users:** Set MySQL to auto-start on boot via `services.msc` → MySQL → Startup type: Automatic.

---

### Running the test suite

```powershell
# Windows
.\.maven\apache-maven-3.9.16\bin\mvn test
```
```bash
# Linux / macOS
./.maven/apache-maven-3.9.16/bin/mvn test
```

Tests cover the text-matching algorithm, Classic mode scoring limits, and Rapid Fire dynamic score deductions.

---


---

*Built with Java Servlets, MySQL, Apache Tomcat, and way too much enthusiasm for Bollywood trivia.*
