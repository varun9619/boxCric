package com.boxai.scorer.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Thin REST client for the FastAPI backend.
 * All calls are suspend functions — run them inside a coroutine scope.
 */
class ApiClient(private val ip: String, private val port: Int = 8000) {

    private val http = HttpClient(CIO)
    private val base = "http://$ip:$port"

    // ── Matches ───────────────────────────────────────────────────────────────

    /** Create a new match; returns the raw JSON response string. */
    suspend fun createMatch(teamA: String, teamB: String, totalOvers: Int): String {
        return try {
            val response: HttpResponse = http.post("$base/matches") {
                contentType(ContentType.Application.Json)
                setBody("""{"team_a":"$teamA","team_b":"$teamB","total_overs":$totalOvers}""")
            }
            response.bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    /** Fetch current match state by ID. */
    suspend fun getMatch(matchId: Int): String {
        return try {
            http.get("$base/matches/$matchId").bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────

    /**
     * POST a confirmed ball event to the backend.
     * @param eventType one of: "run", "boundary", "wicket", "wide", "noball"
     */
    suspend fun postEvent(
        matchId: Int,
        eventType: String,
        runs: Int,
        cameraId: String,
        confidence: Float = 1.0f
    ): String {
        return try {
            val response: HttpResponse = http.post("$base/matches/$matchId/events") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "event_type":"$eventType",
                      "runs":$runs,
                      "camera_id":"$cameraId",
                      "confidence":$confidence
                    }
                    """.trimIndent()
                )
            }
            response.bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    /** Fetch end-of-match summary. */
    suspend fun getMatchSummary(matchId: Int): String {
        return try {
            http.get("$base/matches/$matchId/summary").bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    fun close() { http.close() }
}
