package com.quizapp.servlet;

import com.quizapp.util.JsonUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<String> categories = Arrays.asList(
            "Bollywood",
            "Hollywood",
            "Aviation",
            "Bollywood Music",
            "Technology",
            "Gaming"
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("categories", categories);

        response.getWriter().write(JsonUtils.toJson(result));
    }
}
