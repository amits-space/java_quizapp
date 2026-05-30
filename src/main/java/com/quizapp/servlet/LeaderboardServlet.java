package com.quizapp.servlet;

import com.quizapp.dao.UserScoreDAO;
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

@WebServlet("/api/leaderboard")
public class LeaderboardServlet extends HttpServlet {
    private final UserScoreDAO userScoreDAO = new UserScoreDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        int limit = 10;
        String limitParam = request.getParameter("limit");
        if (limitParam != null) {
            try {
                limit = Integer.parseInt(limitParam);
            } catch (NumberFormatException e) {
                // Ignore and use default limit of 10
            }
        }

        List<UserScore> topScores = userScoreDAO.getTopScores(limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("scores", topScores);

        response.getWriter().write(JsonUtils.toJson(result));
    }
}
