package com.mobitant.myrxjavamodule.api.model

import com.google.gson.annotations.SerializedName

class GithubOwner(
        val login: String,
        @SerializedName("avatar_url") val avatarUrl: String)
