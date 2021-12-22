package com.example.demo.model;

public class Result {
    private String trkId;
    private Double percentageOfSimilarity;

    public Result(String trkId, Double percentageOfSimilarity) {
        this.trkId = trkId;
        this.percentageOfSimilarity = percentageOfSimilarity;
    }

    public String getTrkId() {
        return trkId;
    }

    public void setTrkId(String trkId) {
        this.trkId = trkId;
    }

    public Double getPercentageOfSimilarity() {
        return percentageOfSimilarity;
    }

    public void setPercentageOfSimilarity(Double percentageOfSimilarity) {
        this.percentageOfSimilarity = percentageOfSimilarity;
    }
}
