package com.mobitant.myrxjavamodule.api

import com.mobitant.myrxjavamodule.api.model.GithubRepo
import com.mobitant.myrxjavamodule.api.model.RepoSearchResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {

    @GET("search/repositories")
    //반환타입 변경
    fun searchRepository(@Query("q") query: String): Observable<RepoSearchResponse>

    @GET("repos/{owner}/{name}")
    fun getRepository(
            @Path("owner") ownerLogin: String,
            //반환타입 변경
            @Path("name") repoName: String): Observable<GithubRepo>
}
