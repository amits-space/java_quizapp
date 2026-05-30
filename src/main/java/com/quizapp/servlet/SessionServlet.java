package com.quizapp.servlet;

import com.quizapp.model.GameSession;
import com.quizapp.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/session")
public class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Map<String, Object> result = new HashMap<>();

        if (session == null || session.getAttribute("playerName") == null) {
            result.put("authenticated", false);
        } else {
            result.put("authenticated", true);
            result.put("playerName", session.getAttribute("playerName"));
            
            GameSession gameSession = (GameSession) session.getAttribute("gameSession");
            if (gameSession != null) {
                result.put("hasActiveGame", true);
                result.put("gameMode", gameSession.getMode());
                result.put("category", gameSession.getCategory());
                result.put("currentIndex", gameSession.getCurrentIndex());
                result.put("totalQuestions", gameSession.getQuestions().size());
                
                if ("Rapid Fire".equalsIgnoreCase(gameSession.getMode())) {
                    result.put("remainingSeconds", gameSession.getRemainingSeconds());
                }
            } else {
                result.put("hasActiveGame", false);
            }
        }

        response.getWriter().write(JsonUtils.toJson(result));
    }
}
