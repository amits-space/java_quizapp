package com.quizapp.servlet;

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

@WebServlet("/api/name")
public class NameEntryServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String name = request.getParameter("name");
        Map<String, Object> result = new HashMap<>();

        if (name == null || name.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "Name cannot be empty.");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        name = name.trim();
        if (name.length() > 50) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "Name is too long (max 50 characters).");
            response.getWriter().write(JsonUtils.toJson(result));
            return;
        }

        // Store name in HTTP Session
        HttpSession session = request.getSession(true);
        session.setAttribute("playerName", name);

        result.put("success", true);
        result.put("name", name);
        
        response.getWriter().write(JsonUtils.toJson(result));
    }
}
