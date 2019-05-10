package com.mobitant.myrxjavamodule.api.model

import com.google.gson.annotations.SerializedName

class RepoSearchResponse(
        @SerializedName("total_count") val totalCount: Int,
        val items: List<GithubRepo>)
