package com.quizapp.model;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private String playerName;
    private String mode; // "Classic" or "Rapid Fire"
    private String category;
    private List<Question> questions;
    private int currentIndex;
    private int score;
    private long startTime;
    private long maxDurationMs; // 120000ms for Rapid Fire
    private boolean finished;
    
    // Streaks for dynamic time bonuses (+1s / -1s)
    private int consecutiveCorrect = 0;
    private int consecutiveWrong = 0;
    
    // For Classic mode, we can store player's answers and compute final score at the end
    private List<String> playerAnswers;

    public GameSession() {
        this.questions = new ArrayList<>();
        this.playerAnswers = new ArrayList<>();
        this.currentIndex = 0;
        this.score = 0;
        this.finished = false;
    }

    public GameSession(String playerName, String mode, String category, List<Question> questions) {
        this.playerName = playerName;
        this.mode = mode;
        this.category = category;
        this.questions = questions;
        this.currentIndex = 0;
        this.score = 0;
        this.startTime = System.currentTimeMillis();
        this.finished = false;
        
        if ("Rapid Fire".equalsIgnoreCase(mode)) {
            this.maxDurationMs = 120000; // 120 seconds
        } else {
            this.maxDurationMs = 0; // unlimited or client-controlled (Classic is 10 questions)
        }
        
        this.playerAnswers = new ArrayList<>();
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                this.playerAnswers.add("");
            }
        }
    }

    public boolean isTimeUp() {
        if ("Rapid Fire".equalsIgnoreCase(mode)) {
            long elapsed = System.currentTimeMillis() - startTime;
            // Add a small 2-second grace period for latency
            return elapsed > (maxDurationMs + 2000);
        }
        return false;
    }

    public long getRemainingSeconds() {
        if ("Rapid Fire".equalsIgnoreCase(mode)) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = (maxDurationMs - elapsed) / 1000;
            return Math.max(0, remaining);
        }
        return 0;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
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

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
        this.playerAnswers = new ArrayList<>();
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                this.playerAnswers.add("");
            }
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getMaxDurationMs() {
        return maxDurationMs;
    }

    public void setMaxDurationMs(long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public List<String> getPlayerAnswers() {
        return playerAnswers;
    }

    public void setPlayerAnswer(int index, String answer) {
        if (index >= 0 && index < playerAnswers.size()) {
            playerAnswers.set(index, answer);
        }
    }

    public int registerStreak(boolean isCorrect, boolean isSkip) {
        if (isSkip) {
            consecutiveCorrect = 0;
            consecutiveWrong = 0;
            return 0;
        }
        
        if (isCorrect) {
            consecutiveWrong = 0;
            consecutiveCorrect++;
            if (consecutiveCorrect == 3) {
                consecutiveCorrect = 0;
                // Add +1 second bonus: increases remaining time by shifting startTime forward
                this.startTime += 1000;
                return 1; // +1s adjustment
            }
        } else {
            consecutiveCorrect = 0;
            consecutiveWrong++;
            if (consecutiveWrong == 3) {
                consecutiveWrong = 0;
                // Deduct -1 second penalty: decreases remaining time by shifting startTime backward
                this.startTime -= 1000;
                return -1; // -1s adjustment
            }
        }
        return 0;
    }
}
