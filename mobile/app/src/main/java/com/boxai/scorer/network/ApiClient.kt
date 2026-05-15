package com.boxai.scorer.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.formData
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

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

    // ── Players ───────────────────────────────────────────────────────────────

    suspend fun createPlayer(name: String, teamId: Int): String {
        return try {
            val response: HttpResponse = http.post("$base/players") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"$name","team_id":$teamId}""")
            }
            response.bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    suspend fun uploadFace(playerId: Int, bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            
            val response: HttpResponse = http.submitFormWithBinaryData(
                url = "$base/players/$playerId/register_face",
                formData = formData {
                    append("file", byteArray, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"face.jpg\"")
                    })
                }
            )
            response.bodyAsText()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    suspend fun recognizeBatter(matchId: Int, bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            
            val response: HttpResponse = http.submitFormWithBinaryData(
                url = "$base/matches/$matchId/recognize_batter",
                formData = formData {
                    append("file", byteArray, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"batter.jpg\"")
                    })
                }
            )
            response.bodyAsText()
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
