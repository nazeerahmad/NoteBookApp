package com.example.notebookapp.network.model;

import com.google.gson.annotations.SerializedName;

public class User extends BaseResponse {

    @SerializedName("api_key")
    private

    String api_key;

    public String getApi_key() {
        return api_key;
    }
}
