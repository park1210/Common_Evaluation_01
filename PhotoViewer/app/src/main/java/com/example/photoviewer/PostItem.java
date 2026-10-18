package com.example.photoviewer;

import android.graphics.Bitmap;


public class PostItem {

    private final String title;
    private final String text;
    private final String publishedDate;
    private final Bitmap bitmap;


    public PostItem(
            String title,
            String text,
            String publishedDate,
            Bitmap bitmap
    ) {

        this.title = title;
        this.text = text;
        this.publishedDate = publishedDate;
        this.bitmap = bitmap;
    }


    public String getTitle() {

        return title;
    }


    public String getText() {

        return text;
    }


    public String getPublishedDate() {

        return publishedDate;
    }


    public Bitmap getBitmap() {

        return bitmap;
    }
}