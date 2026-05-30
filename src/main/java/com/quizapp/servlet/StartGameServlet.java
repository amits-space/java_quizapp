package com.quizapp.servlet;

import com.quizapp.dao.QuestionDAO;
import com.quizapp.model.GameSession;
import com.quizapp.model.Question;
import com.quizapp.util.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/start")
public class StartGameServlet extends HttpServlet {
    private final QuestionDAO questionDAO = new QuestionDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Map<String, Object> result = new HashMap<>();

        if (session == null || session.getAttribute("playerName") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("success", false);
            result.put("error", "Player session not initialized. Please set player name first.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        String playerName = (String) session.getAttribute("playerName");
        String mode = request.getParameter("mode"); // "Classic" or "Rapid Fire"
        String category = request.getParameter("category");

        if (mode == null || (!"Classic".equalsIgnoreCase(mode) && !"Rapid Fire".equalsIgnoreCase(mode))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "Invalid or missing game mode. Allowed: 'Classic', 'Rapid Fire'");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        if (category == null || category.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "Category must be specified.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        // Fetch questions from database and shuffle
        List<Question> questions = questionDAO.getQuestionsByCategory(category);
        if (questions == null || questions.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("error", "No questions found for category: " + category);
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        Collections.shuffle(questions);

        // Slice for Classic mode to exactly 10 questions (or less if 10 not available)
        if ("Classic".equalsIgnoreCase(mode) && questions.size() > 10) {
            questions = questions.subList(0, 10);
        }

        // Initialize game session
        GameSession gameSession = new GameSession(playerName, mode, category, questions);
        session.setAttribute("gameSession", gameSession);

        result.put("success", true);
        result.put("totalQuestions", questions.size());
        result.put("currentIndex", 0);
        result.put("mode", gameSession.getMode());
        result.put("category", gameSession.getCategory());
        
        // Return first question (sanitized to remove the correct answer)
        Question firstQuestion = questions.get(0);
        result.put("question", sanitizeQuestion(firstQuestion));

        response.getWriter().write(JsonUtils.toJson(result));
    }

    public static Map<String, Object> sanitizeQuestion(Question q) {
        Map<String, Object> sanitized = new HashMap<>();
        sanitized.put("id", q.getId());
        sanitized.put("category", q.getCategory());
        sanitized.put("question", q.getQuestionText());
        
        List<String> options = q.getOptions();
        if (options != null) {
            List<String> shuffled = new java.util.ArrayList<>(options);
            Collections.shuffle(shuffled);
            sanitized.put("options", shuffled);
        } else {
            sanitized.put("options", new java.util.ArrayList<>());
        }
        
        sanitized.put("difficulty", q.getDifficulty());
        return sanitized;
    }
}
