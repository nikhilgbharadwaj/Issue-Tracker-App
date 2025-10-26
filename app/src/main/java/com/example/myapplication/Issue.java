package com.example.myapplication;

public class Issue {
    public String title, description, imageUrl, status;
    public double latitude, longitude;

    public Issue() {} // Required for Firebase

    public Issue(String title, String description, double latitude, double longitude, String imageUrl, String status) {
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.status = status;
    }
}
