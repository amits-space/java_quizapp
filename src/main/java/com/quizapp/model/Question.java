package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {
    private int id;
    private String category;
    
    @JsonProperty("question")
    private String questionText;
    
    @JsonProperty("answer")
    private String answerText;
    
    private List<String> options;
    private String difficulty;
    private String tags;

    public Question() {}

    public Question(int id, String category, String questionText, String answerText, List<String> options, String difficulty, String tags) {
        this.id = id;
        this.category = category;
        this.questionText = questionText;
        this.answerText = answerText;
        this.options = options;
        this.difficulty = difficulty;
        this.tags = tags;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @JsonProperty("tags")
    public void setTagsFromJson(List<String> tagsList) {
        if (tagsList != null) {
            this.tags = String.join(",", tagsList);
        } else {
            this.tags = null;
        }
    }
}
