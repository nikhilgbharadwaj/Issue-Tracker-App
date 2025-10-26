package com.example.myapplication;

public class IssueModel {
    public String issueId, title, description, imageUrl, latitude, longitude, status;

    public IssueModel() {}

    public IssueModel(String issueId, String title, String description, String imageUrl,
                      String latitude, String longitude, String status) {
        this.issueId = issueId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }
}
