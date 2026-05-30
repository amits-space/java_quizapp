# MindGlow Quiz Portal

A responsive, high-fidelity **Java-Servlet Quiz Portal** featuring a lightweight MVC architecture, MySQL persistence, robust session-based score validation, and a stunning, responsive Pastel UI. Ready for deployment on Apache Tomcat in Bengaluru (IST).

---

## ⚡ Key Highlights
* **Zero-Touch Database Initialization:** Features a `ServletContextListener` that automatically checks for the existence of `quizdb` database and its tables on startup, seeding it with a rich bank of **120+ curated questions** across 6 categories from a classpath JSON resource.
* **Dual Theme Custom Styling:** Implements the custom Pastel styling tokens for both Light mode (very light warm yellow `#FFF9E6` with soft teal `#6FBF9A` accents) and Dark mode (muted slate graphite `#1F1F23` with soft blue `#8AB4FF` accents) with hardware-accelerated animations.
* **Dual Game Playstyles:**
  * **Classic Mode:** 10 randomized multiple-choice questions (MCQs), unlimited time, +4/-1 scoring with 0 penalty on skipped/passed options.
  * **Rapid Fire:** Intense 90-second race with free-text case-insensitive input field matching, +4 correct, -1 wrong, and -1 penalty on skips (with an interactive warning panel!).
* **Leaderboard:** Ranked global leaderboard with Gold/Silver/Bronze crown/badge decorations, showing dates and times synced in Indian Standard Time (IST).
* **Session Security:** Quiz answers and timer clocks are fully validated server-side to block browser inspection cheating. Player badges and game states persist through unexpected page refreshes.

---

## 📂 Project Architecture & Code Structure

```
├── .maven/                             <-- Local Maven installation binary
├── pom.xml                             <-- Maven dependencies and compilation setup
├── README.md                           <-- Run and instruction manual
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── quizapp
│   │   │           ├── dao             <-- ConnectionPool, QuestionDAO, UserScoreDAO
│   │   │           ├── model           <-- Question, UserScore, GameSession
│   │   │           ├── servlet         <-- NameEntryServlet, StartGameServlet, AnswerServlet, etc.
│   │   │           └── util            <-- ConfigLoader, JsonUtils
│   │   ├── resources
│   │   │   ├── config.properties       <-- DB URL, credentials, and HikariCP pool configurations
│   │   │   └── data
│   │   │       └── questions
│   │   │           └── questions.json  <-- 120-item question seed bank
│   │   └── webapp
│   │       ├── css
│   │       │   └── style.css           <-- Pastel design system stylesheet
│   │       ├── js
│   │       │   └── app.js              <-- Front-end SPA router and canvas confetti particle logic
│   │       ├── WEB-INF
│   │       │   └── web.xml             <-- Deployment mappings
│   │       └── index.html              <-- Single Page Application viewport HTML5
│   └── test
│       └── java
│           └── com
│               └── quizapp
│                   └── ServiceTest.java <-- JUnit automated unit tests
```

---

## ⚙️ Prerequisites & Configuration
1. **Java:** JDK 11 or higher installed on your system.
2. **Database:** An active instance of **MySQL** (or MariaDB) listening on `localhost:3306`.
3. **Database Credentials:** By default, the application connects using username `root` and **no password**. If your server has credentials, modify them in `src/main/resources/config.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/quizdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
   db.username=YOUR_MYSQL_USERNAME
   db.password=YOUR_MYSQL_PASSWORD
   ```

---

## 🚀 Execution & Running Instructions

### ⚡ Recommended: One-Click Automated Startup & Shutdown
We have included a highly professional, interactive command-line manager utility, `run.bat`, at the root of the project. This utility eliminates the need to manually start database servers or deploy WAR files.

To run the application:
1. Double-click the **`run.bat`** file in your project folder, or run `.\run.bat` in your terminal.
2. Select **`[1]`** to perform a full clean-build, boot your portable MySQL server and Apache Tomcat, automatically deploy the `quizapp.war` file, and open the quiz portal in your default browser.
3. Select **`[2]`** to bypass the Maven build and start the application instantly with the last compiled bundle.
4. Select **`[3]`** when you are done to gracefully shut down both the Tomcat web server and the MySQL database server, ensuring zero resource leaks or database corruptions!

---

### Manual Deployment (Optional)

### 1. Build and Compile the Application (WAR file)
To package the portal into a deployable Web Archive (`WAR`) file, run the local maven wrapper command in your terminal:
```powershell
# Windows PowerShell
.\.maven\apache-maven-3.9.16\bin\mvn clean package
```
This produces a deployable file `target/quizapp.war` in the project folder.

### 2. Deployment on Apache Tomcat
1. Copy the compiled `quizapp.war` from the `target/` directory.
2. Paste it into the `webapps/` folder of your Apache Tomcat installation.
3. Start Tomcat using `bin/startup.bat` (Windows) or `bin/startup.sh` (Linux/Mac).
4. Access the web portal in your browser at: `http://localhost:8080/quizapp/`

*(Note: On Tomcat startup, the schema will be initialized, the `quizdb` database created, and 120 questions seeded automatically. Zero manual database imports required!)*

---

## 🧪 Automated Testing
To run the automated test suite testing the text-matching algorithms, Classic mode scoring limits, and Rapid Fire dynamic scoring deductions:
```powershell
.\.maven\apache-maven-3.9.16\bin\mvn test
```
All unit tests are integrated and verified!
