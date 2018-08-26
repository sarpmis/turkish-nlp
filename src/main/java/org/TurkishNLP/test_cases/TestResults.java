package org.TurkishNLP.test_cases;

public class TestResults {
    private Double score;
    private String message;

    public TestResults() {
        score = Double.NaN;
        message = null;
    }

    public TestResults setScore(double score) {
        this.score = score;
        return this;
    }

    public TestResults setMessage(String message) {
        this.message = message;
        return this;
    }

    public Double getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }
}

