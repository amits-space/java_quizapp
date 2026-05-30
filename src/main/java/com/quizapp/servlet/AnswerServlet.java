package com.quizapp.servlet;

import com.quizapp.model.GameSession;
import com.quizapp.model.Question;
import com.quizapp.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/answer")
public class AnswerServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Map<String, Object> result = new HashMap<>();

        if (session == null || session.getAttribute("gameSession") == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "No active game session found.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        GameSession gameSession = (GameSession) session.getAttribute("gameSession");
        
        // 1. Server-side timer check for Rapid Fire
        if ("Rapid Fire".equalsIgnoreCase(gameSession.getMode()) && gameSession.isTimeUp()) {
            gameSession.setFinished(true);
            result.put("success", false);
            result.put("timeUp", true);
            result.put("score", gameSession.getScore());
            result.put("message", "Time is up! Answers submitted after 120 seconds are not counted.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        int currentIndex = gameSession.getCurrentIndex();
        if (currentIndex >= gameSession.getQuestions().size()) {
            result.put("success", true);
            result.put("finished", true);
            result.put("score", gameSession.getScore());
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        Question currentQuestion = gameSession.getQuestions().get(currentIndex);
        String playerAnswer = request.getParameter("answer");
        boolean isSkip = "true".equalsIgnoreCase(request.getParameter("skip"));

        boolean isCorrect = false;
        String dbAnswer = currentQuestion.getAnswerText();

        if ("Rapid Fire".equalsIgnoreCase(gameSession.getMode())) {
            // Rapid Fire Mode Scoring: +4 correct, -1 wrong, -1 skip
            int timeAdjustment = 0;
            if (isSkip) {
                gameSession.setScore(gameSession.getScore() - 1);
                isCorrect = false;
                result.put("feedback", "skipped");
                timeAdjustment = gameSession.registerStreak(false, true);
            } else {
                isCorrect = checkAnswer(playerAnswer, dbAnswer, true);
                if (isCorrect) {
                    gameSession.setScore(gameSession.getScore() + 4);
                    result.put("feedback", "correct");
                    timeAdjustment = gameSession.registerStreak(true, false);
                } else {
                    gameSession.setScore(gameSession.getScore() - 1);
                    result.put("feedback", "wrong");
                    timeAdjustment = gameSession.registerStreak(false, false);
                }
            }
            result.put("correctAnswer", dbAnswer);
            result.put("score", gameSession.getScore());
            result.put("timeAdjustment", timeAdjustment);
            result.put("remainingSeconds", gameSession.getRemainingSeconds());
        } else {
            // Classic Mode: evaluate immediately to support dynamic HUD updates and tactile visual feedback!
            if (isSkip) {
                gameSession.setPlayerAnswer(currentIndex, "");
                isCorrect = false;
                result.put("feedback", "skipped");
            } else {
                gameSession.setPlayerAnswer(currentIndex, playerAnswer != null ? playerAnswer.trim() : "");
                isCorrect = checkAnswer(playerAnswer, dbAnswer);
                if (isCorrect) {
                    gameSession.setScore(gameSession.getScore() + 4);
                    result.put("feedback", "correct");
                } else {
                    gameSession.setScore(gameSession.getScore() - 1);
                    result.put("feedback", "wrong");
                }
            }
            result.put("correctAnswer", dbAnswer);
            result.put("score", gameSession.getScore());
        }

        // Move to the next question
        int nextIndex = currentIndex + 1;
        gameSession.setCurrentIndex(nextIndex);

        // Check if game is complete (all questions answered)
        if (nextIndex >= gameSession.getQuestions().size()) {
            if ("Rapid Fire".equalsIgnoreCase(gameSession.getMode())) {
                gameSession.setFinished(true);
            }
            result.put("success", true);
            result.put("finished", true);
            result.put("score", gameSession.getScore());
        } else {
            result.put("success", true);
            result.put("finished", false);
            result.put("currentIndex", nextIndex);
            
            // Return next sanitized question
            Question nextQuestion = gameSession.getQuestions().get(nextIndex);
            result.put("question", StartGameServlet.sanitizeQuestion(nextQuestion));
        }

        response.getWriter().write(JsonUtils.toJson(result));
    }

    /**
     * Case-insensitive matching logic that is highly robust.
     * Incorporates: exact match, word-order-independent match, acronym generation, and Levenshtein spelling tolerance.
     */
    public static boolean checkAnswer(String playerAns, String dbAns) {
        return checkAnswer(playerAns, dbAns, false);
    }

    /**
     * Case-insensitive matching logic that is highly robust.
     * Incorporates: exact match, word-order-independent match, acronym generation, and Levenshtein spelling tolerance.
     */
    public static boolean checkAnswer(String playerAns, String dbAns, boolean isRapid) {
        if (playerAns == null || dbAns == null) {
            return false;
        }
        
        // 1. Canonicalize inputs
        String p = playerAns.trim().toLowerCase().replaceAll("\\s+", " ");
        String d = dbAns.trim().toLowerCase().replaceAll("\\s+", " ");
        
        // Remove basic single/double quotes at boundary if present
        p = stripQuotes(p);
        d = stripQuotes(d);
        
        // A. Standard exact case-insensitive match
        if (p.equals(d)) {
            return true;
        }
        
        if (!isRapid) {
            return false;
        }
        
        // B. Acronym match (e.g. "ddlj" matches "Dilwale Dulhania Le Jayenge")
        String acronym = getAcronym(d);
        if (!acronym.isEmpty() && p.equals(acronym)) {
            return true;
        }
        
        // C. Word-order-independent match with spelling tolerance
        if (checkWordOrderIndependentMatch(p, d)) {
            return true;
        }
        
        // D. Fuzzy spelling match using Damerau-Levenshtein distance on the whole string
        if (checkFuzzyMatch(p, d)) {
            return true;
        }
        
        return false;
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("'") && s.endsWith("'")) {
            s = s.substring(1, s.length() - 1);
        } else if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.trim();
    }

    private static String getAcronym(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            String cleanW = w.replaceAll("\\W+", "");
            if (!cleanW.isEmpty()) {
                sb.append(cleanW.charAt(0));
            }
        }
        return sb.toString().toLowerCase();
    }

    private static boolean checkWordOrderIndependentMatch(String p, String d) {
        List<String> pWords = cleanWords(p);
        List<String> dWords = cleanWords(d);
        
        if (pWords.isEmpty() || dWords.isEmpty() || pWords.size() != dWords.size()) {
            return false;
        }
        
        // For each word in dWords, try to find a match in pWords
        // We'll track matched indices in pWords
        boolean[] pMatched = new boolean[pWords.size()];
        
        for (String dw : dWords) {
            boolean found = false;
            // 1. Try to find exact match first
            for (int i = 0; i < pWords.size(); i++) {
                if (!pMatched[i] && pWords.get(i).equals(dw)) {
                    pMatched[i] = true;
                    found = true;
                    break;
                }
            }
            if (found) continue;
            
            // 2. If no exact match, try to find a fuzzy match
            for (int i = 0; i < pWords.size(); i++) {
                if (!pMatched[i] && checkWordFuzzyMatch(pWords.get(i), dw)) {
                    pMatched[i] = true;
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    private static List<String> cleanWords(String s) {
        String[] words = s.split("[\\s\\p{Punct}]+");
        List<String> list = new ArrayList<>();
        for (String w : words) {
            String clean = w.toLowerCase().trim();
            // Ignore common connectors, prepositions, and articles
            if (!clean.isEmpty() && !isIgnoredWord(clean)) {
                list.add(clean);
            }
        }
        return list;
    }
    
    private static boolean isIgnoredWord(String s) {
        String[] ignored = {
            "and", "or", "the", "a", "an", "of", "to", "in", "for", "with", "on", "at", "by", "from", "le", "la", "les", "du", "de", "&"
        };
        for (String ig : ignored) {
            if (ig.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkWordFuzzyMatch(String pw, String dw) {
        int len = dw.length();
        if (len <= 3) {
            return false; // exact match required for short words
        }
        int distance = getDamerauLevenshteinDistance(pw, dw);
        int allowed = (len >= 9) ? 2 : 1;
        return distance <= allowed;
    }

    private static boolean checkFuzzyMatch(String p, String d) {
        if (d.length() <= 3) {
            return false;
        }
        int distance = getDamerauLevenshteinDistance(p, d);
        int allowedDistance = (d.length() >= 9) ? 2 : 1;
        return distance <= allowedDistance;
    }

    private static int getDamerauLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1),      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
                if (i > 1 && j > 1 && s1.charAt(i - 1) == s2.charAt(j - 2) && s1.charAt(i - 2) == s2.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + cost); // transposition
                }
            }
        }
        return dp[len1][len2];
    }
}
