package com.passion.knownow.data.network

import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {
    @GET("repos/{owner}/{repo}")
    suspend fun repoRaw(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): String

    @GET("repos/{owner}/{repo}/branches")
    suspend fun branchesRaw(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
    ): String
}

data class BranchDto(val name: String, val sha: String)

fun parseBranches(json: String): List<BranchDto> {
    val arr = JSONArray(json)
    val out = mutableListOf<BranchDto>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val commit = o.getJSONObject("commit")
        out += BranchDto(o.getString("name"), commit.getString("sha"))
    }
    return out
}

fun parseDefaultBranch(json: String): String? = runCatching {
    JSONObject(json).optString("default_branch").takeIf { it.isNotBlank() }
}.getOrNull()
