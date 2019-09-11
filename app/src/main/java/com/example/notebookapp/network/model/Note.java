package com.example.notebookapp.network.model;

public class Note extends BaseResponse {
    private int id;
    private String note;
    private String timestamp;

    public void setNote(String note) {
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
