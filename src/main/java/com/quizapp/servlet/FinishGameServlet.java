package com.quizapp.servlet;

import com.quizapp.dao.UserScoreDAO;
import com.quizapp.model.GameSession;
import com.quizapp.model.Question;
import com.quizapp.model.UserScore;
import com.quizapp.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/finish")
public class FinishGameServlet extends HttpServlet {
    private final UserScoreDAO userScoreDAO = new UserScoreDAO();

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
            result.put("error", "No active game session found to finish.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        GameSession gameSession = (GameSession) session.getAttribute("gameSession");
        
        // 1. Calculate Classic Mode score at the end
        if ("Classic".equalsIgnoreCase(gameSession.getMode())) {
            int score = 0;
            List<Question> questions = gameSession.getQuestions();
            List<String> answers = gameSession.getPlayerAnswers();
            
            for (int i = 0; i < questions.size(); i++) {
                String playerAns = (i < answers.size()) ? answers.get(i) : "";
                String correctAns = questions.get(i).getAnswerText();
                
                if (playerAns == null || playerAns.trim().isEmpty()) {
                    // Passed / Skipped -> 0 points (no penalty)
                    score += 0;
                } else if (AnswerServlet.checkAnswer(playerAns, correctAns)) {
                    // Correct -> +4 points
                    score += 4;
                } else {
                    // Wrong -> -1 point
                    score -= 1;
                }
            }
            gameSession.setScore(score);
        }

        // 2. Build UserScore entity and persist
        UserScore userScore = new UserScore();
        userScore.setPlayerName(gameSession.getPlayerName());
        userScore.setScore(gameSession.getScore());
        userScore.setMode(gameSession.getMode());
        userScore.setCategory(gameSession.getCategory());
        
        userScoreDAO.insertScore(userScore);

        // 3. Clear active game session from the HttpSession (leaves player name)
        session.removeAttribute("gameSession");

        // 4. Return results
        result.put("success", true);
        result.put("playerName", userScore.getPlayerName());
        result.put("score", userScore.getScore());
        result.put("mode", userScore.getMode());
        result.put("category", userScore.getCategory());

        response.getWriter().write(JsonUtils.toJson(result));
    }
}
