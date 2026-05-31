package com.quizapp.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quizapp.model.Question;
import com.quizapp.util.JsonUtils;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DatabaseInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("QuizApp web application starting up. Initializing database schema...");
        
        // This triggers the static block in ConnectionPool, creating the DB if needed.
        try (Connection conn = ConnectionPool.getConnection()) {
            
            // Create tables if they do not exist
            createTables(conn);
            
            // Seed questions if empty
            seedQuestionsIfEmpty();
            
            System.out.println("Database initialization completed successfully.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during database initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) {
        String createQuestionsTable = "CREATE TABLE IF NOT EXISTS questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "category VARCHAR(50) NOT NULL,"
                + "question_text TEXT NOT NULL,"
                + "answer_text VARCHAR(255) NOT NULL,"
                + "options_json TEXT,"
                + "difficulty VARCHAR(20) DEFAULT 'medium',"
                + "tags VARCHAR(255)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String createUserScoresTable = "CREATE TABLE IF NOT EXISTS user_scores ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "player_name VARCHAR(100) NOT NULL,"
                + "score INT NOT NULL,"
                + "mode VARCHAR(20) NOT NULL,"
                + "category VARCHAR(50) NOT NULL,"
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createQuestionsTable);
            stmt.executeUpdate(createUserScoresTable);
            System.out.println("Verified/Created database tables 'questions' and 'user_scores'.");
        } catch (Exception e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedQuestionsIfEmpty() {
        QuestionDAO questionDAO = new QuestionDAO();
        System.out.println("Syncing questions from questions.json to database...");

        // Clear the table to ensure any added/modified/deleted questions are fully synchronized
        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM questions");
            System.out.println("Cleared existing questions table to perform clean synchronization from questions.json.");
        } catch (Exception e) {
            System.err.println("Warning: Failed to clear questions table before syncing: " + e.getMessage());
        }

        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/questions/questions.json");
        if (input == null) {
            input = DatabaseInitializer.class.getClassLoader().getResourceAsStream("data/questions/questions.json");
        }
        if (input == null) {
            input = DatabaseInitializer.class.getResourceAsStream("/data/questions/questions.json");
        }

        if (input == null) {
            System.err.println("Error: questions.json not found in classpath! Unable to seed.");
            return;
        }

        try (InputStream autoCloseInput = input) {
            List<Question> questions = JsonUtils.fromJson(autoCloseInput, new TypeReference<List<Question>>() {});
            if (questions != null && !questions.isEmpty()) {
                questionDAO.insertNewQuestions(questions);
            } else {
                System.err.println("Warning: parsed questions list is empty or null.");
            }
        } catch (Exception e) {
            System.err.println("Error seeding questions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("QuizApp web application shutting down. Cleaning up connections...");
        ConnectionPool.shutdown();
    }
}
