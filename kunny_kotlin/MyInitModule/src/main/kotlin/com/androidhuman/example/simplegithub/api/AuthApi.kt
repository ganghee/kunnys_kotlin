package com.androidhuman.example.simplegithub.api

import com.androidhuman.example.simplegithub.api.model.GithubAccessToken

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    fun getAccessToken(
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            //Call 객체를 Observable로 바꿈
            @Field("code") code: String): Call<GithubAccessToken>
}
