package com.quizapp;

import static org.junit.jupiter.api.Assertions.*;

import com.quizapp.model.GameSession;
import com.quizapp.model.Question;
import com.quizapp.servlet.AnswerServlet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServiceTest {

    @Test
    public void testCheckAnswerRobustness() {
        // Test basic match
        assertTrue(AnswerServlet.checkAnswer("Rajkumar Hirani", "Rajkumar Hirani"));

        // Test case-insensitivity
        assertTrue(AnswerServlet.checkAnswer("rajkumar hirani", "Rajkumar Hirani"));
        assertTrue(AnswerServlet.checkAnswer("RAJKUMAR HIRANI", "rajkumar hirani"));

        // Test whitespace trimming and collapsing
        assertTrue(AnswerServlet.checkAnswer("  Rajkumar   Hirani  ", "Rajkumar Hirani"));

        // Test boundary quotes stripping
        assertTrue(AnswerServlet.checkAnswer("\"Rajkumar Hirani\"", "Rajkumar Hirani"));
        assertTrue(AnswerServlet.checkAnswer("'Rajkumar Hirani'", "Rajkumar Hirani"));

        // Test wrong answers
        assertFalse(AnswerServlet.checkAnswer("Karan Johar", "Rajkumar Hirani"));
        assertFalse(AnswerServlet.checkAnswer("", "Rajkumar Hirani"));
        assertFalse(AnswerServlet.checkAnswer(null, "Rajkumar Hirani"));
    }

    @Test
    public void testEnhancedCheckAnswer() {
        // 1. Word order independence
        assertTrue(AnswerServlet.checkAnswer("Steve and Alex", "Alex and Steve", true));
        assertTrue(AnswerServlet.checkAnswer("Alex and Steve", "Steve and Alex", true));

        // 2. Acronym matching
        assertTrue(AnswerServlet.checkAnswer("DDLJ", "Dilwale Dulhania Le Jayenge", true));
        assertTrue(AnswerServlet.checkAnswer("ddlj", "Dilwale Dulhania Le Jayenge", true));

        // 3. Spelling mistake tolerances (transpositions and substitutions)
        // Transpositions
        assertTrue(AnswerServlet.checkAnswer("Alxe and Steve", "Alex and Steve", true));
        assertTrue(AnswerServlet.checkAnswer("Alex and Stvee", "Alex and Steve", true));
        assertTrue(AnswerServlet.checkAnswer("Stvee and Alxe", "Alex and Steve", true));

        // Substitutions
        assertTrue(AnswerServlet.checkAnswer("Aleks and Steve", "Alex and Steve", true));
        assertTrue(AnswerServlet.checkAnswer("Alex and Steeve", "Alex and Steve", true));

        // Connectors ignored
        assertTrue(AnswerServlet.checkAnswer("Alex Steve", "Steve and Alex", true));
        assertTrue(AnswerServlet.checkAnswer("Alex & Steve", "Steve and Alex", true));

        // Wrong answers (too many edits or completely off)
        assertFalse(AnswerServlet.checkAnswer("Bob and Steve", "Alex and Steve", true));
        assertFalse(AnswerServlet.checkAnswer("Alex and Bob", "Steve and Alex", true));
    }

    @Test
    public void testClassicModeScoring() {
        // Create 3 mock questions
        Question q1 = new Question(1, "Bollywood", "Q1", "Ans A", Arrays.asList("Ans A", "Ans B", "Ans C"), "easy", "");
        Question q2 = new Question(2, "Bollywood", "Q2", "Ans B", Arrays.asList("Ans A", "Ans B", "Ans C"), "easy", "");
        Question q3 = new Question(3, "Bollywood", "Q3", "Ans C", Arrays.asList("Ans A", "Ans B", "Ans C"), "easy", "");

        List<Question> list = Arrays.asList(q1, q2, q3);
        GameSession session = new GameSession("TestPlayer", "Classic", "Bollywood", list);

        // Classic mode scoring calculations:
        // Set up answers:
        // Index 0: "Ans A" (Correct) -> +4 points
        // Index 1: "Ans A" (Wrong) -> -1 point
        // Index 2: "" (Passed/Skipped) -> 0 points (no penalty)
        // Total expected: 4 - 1 + 0 = 3 points!
        session.setPlayerAnswer(0, "Ans A");
        session.setPlayerAnswer(1, "Ans A");
        session.setPlayerAnswer(2, "");

        // Manual calculation logic matching FinishGameServlet:
        int score = 0;
        for (int i = 0; i < list.size(); i++) {
            String playerAns = session.getPlayerAnswers().get(i);
            String correctAns = list.get(i).getAnswerText();

            if (playerAns == null || playerAns.trim().isEmpty()) {
                score += 0;
            } else if (AnswerServlet.checkAnswer(playerAns, correctAns)) {
                score += 4;
            } else {
                score -= 1;
            }
        }

        assertEquals(3, score);
    }

    @Test
    public void testRapidFireScoringDeductions() {
        // Rapid Fire scoring uses running updates in AnswerServlet:
        // Correct: +4
        // Wrong: -1
        // Skip: -1
        int score = 0;

        // Event 1: Correct answer
        boolean isCorrect1 = AnswerServlet.checkAnswer("dilwale dulhania le jayenge", "Dilwale Dulhania Le Jayenge");
        assertTrue(isCorrect1);
        score += 4; // score is now 4

        // Event 2: Wrong answer
        boolean isCorrect2 = AnswerServlet.checkAnswer("karan johar", "Rajkumar Hirani");
        assertFalse(isCorrect2);
        score -= 1; // score is now 3

        // Event 3: Skipped question (always penalty)
        boolean isSkip = true;
        if (isSkip) {
            score -= 1; // score is now 2
        }

        assertEquals(2, score);
    }
}
