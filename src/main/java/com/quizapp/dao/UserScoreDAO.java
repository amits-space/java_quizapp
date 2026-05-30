package com.quizapp.dao;

import com.quizapp.model.UserScore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserScoreDAO {

    public void insertScore(UserScore score) {
        String sql = "INSERT INTO user_scores (player_name, score, mode, category) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, score.getPlayerName());
            ps.setInt(2, score.getScore());
            ps.setString(3, score.getMode());
            ps.setString(4, score.getCategory());
            
            ps.executeUpdate();
            System.out.println("Score persisted successfully for player: " + score.getPlayerName() + " (Score: " + score.getScore() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<UserScore> getTopScores(int limit) {
        List<UserScore> scores = new ArrayList<>();
        String sql = "SELECT * FROM user_scores ORDER BY score DESC, timestamp DESC LIMIT ?";
        
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserScore us = new UserScore();
                    us.setId(rs.getInt("id"));
                    us.setPlayerName(rs.getString("player_name"));
                    us.setScore(rs.getInt("score"));
                    us.setMode(rs.getString("mode"));
                    us.setCategory(rs.getString("category"));
                    us.setTimestamp(rs.getTimestamp("timestamp"));
                    scores.add(us);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scores;
    }
}
