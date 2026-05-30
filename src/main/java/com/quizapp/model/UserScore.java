package com.quizapp.model;

import java.sql.Timestamp;

public class UserScore {
    private int id;
    private String playerName;
    private int score;
    private String mode;
    private String category;
    private Timestamp timestamp;

    public UserScore() {}

    public UserScore(int id, String playerName, int score, String mode, String category, Timestamp timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.mode = mode;
        this.category = category;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
