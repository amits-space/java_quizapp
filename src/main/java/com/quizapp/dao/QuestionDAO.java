package com.quizapp.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quizapp.model.Question;
import com.quizapp.util.JsonUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    public List<Question> getQuestionsByCategory(String category) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE LOWER(category) = LOWER(?)";
        
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setId(rs.getInt("id"));
                    q.setCategory(rs.getString("category"));
                    q.setQuestionText(rs.getString("question_text"));
                    q.setAnswerText(rs.getString("answer_text"));
                    
                    String optJson = rs.getString("options_json");
                    if (optJson != null && !optJson.trim().isEmpty()) {
                        List<String> options = JsonUtils.fromJson(optJson, new TypeReference<List<String>>() {});
                        q.setOptions(options);
                    }
                    
                    q.setDifficulty(rs.getString("difficulty"));
                    q.setTags(rs.getString("tags"));
                    questions.add(q);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    public int getQuestionCount() {
        String sql = "SELECT COUNT(*) FROM questions";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void insertQuestions(List<Question> questions) {
        String sql = "INSERT INTO questions (category, question_text, answer_text, options_json, difficulty, tags) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            for (Question q : questions) {
                ps.setString(1, q.getCategory());
                ps.setString(2, q.getQuestionText());
                ps.setString(3, q.getAnswerText());
                ps.setString(4, JsonUtils.toJson(q.getOptions()));
                ps.setString(5, q.getDifficulty());
                ps.setString(6, q.getTags());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            System.out.println("Inserted " + questions.size() + " questions successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
