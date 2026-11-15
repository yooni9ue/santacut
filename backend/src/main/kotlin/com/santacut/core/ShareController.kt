package com.santacut.core

import com.santacut.api.docs.CreateShareApi
import com.santacut.api.docs.GetMyResultApi
import com.santacut.api.docs.GetRankingsApi
import com.santacut.api.docs.PublicShareImageApi
import com.santacut.api.docs.PublicSharePageApi
import com.santacut.api.docs.RevokeShareApi
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.imageio.ImageIO

data class MyResultResponse(
    @field:Schema(description = "Competition rank; equal scores share a rank") val rank: Int,
    @field:JsonProperty("delivery_sequence") @field:Schema(description = "Delivery order among participants") val deliverySequence: Int,
    @field:Schema(description = "Simulated delivery ETA in UTC", format = "date-time") val eta: String,
    @field:JsonProperty("computed_at") @field:Schema(description = "Time the ranking snapshot was calculated in UTC", format = "date-time") val computedAt: String,
    @field:JsonProperty("score_hundredths") @field:Schema(description = "Weighted score in hundredths; divide by 100 for points", example = "8245") val scoreHundredths: Int,
    @field:Schema(description = "Playful score verdict") val verdict: String,
    @field:Schema(description = "Playful explanation") val comment: String
)

data class RankingResponse(
    @field:Schema(description = "Participant nickname") val nickname: String,
    @field:Schema(description = "Competition rank; equal scores share a rank") val rank: Int,
    @field:JsonProperty("score_hundredths") @field:Schema(description = "Weighted score in hundredths; divide by 100 for points") val scoreHundredths: Int,
    @field:Schema(description = "Simulated delivery ETA in UTC", format = "date-time") val eta: String,
    @field:JsonProperty("computed_at") @field:Schema(description = "Time the ranking snapshot was calculated in UTC", format = "date-time") val computedAt: String
)

data class CreateShareResponse(
    @field:Schema(description = "43-character base64url bearer token. Keep private; anyone with it can view the result.") val token: String,
    @field:Schema(description = "Public HTML result page URL", format = "uri") val url: String,
    @field:Schema(description = "Public 1200×630 PNG card URL", format = "uri") val imageUrl: String
)

private data class SharePageData(
    val nickname: String,
    @field:JsonProperty("score_hundredths") val scoreHundredths: Int,
    val rank: Int,
    val eta: String,
    val verdict: String,
    val comment: String,
    @field:JsonProperty("computed_at") val computedAt: String
)

fun escapeHtml(text: String): String = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;")

@RestController
class ShareController(
    private val snapshots: ResultSnapshotRepository,
    private val assessments: AssessmentRepository,
    private val participants: ParticipantRepository,
    private val shares: ShareRepository,
    @Value("\${app.public-base-url}") baseUrl: String
) {
    private val base = baseUrl.trimEnd('/').also {
        val uri = URI(it)
        require(uri.scheme in setOf("http","https") && uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty()) { "APP_PUBLIC_BASE_URL must be an origin URL" }
    }
    @GetMapping("/v1/me/result")
    @GetMyResultApi
    fun result(auth: Authentication): MyResultResponse {
        val userId=(auth.principal as UUID).toString()
        val snapshot=snapshots.findFirstByUserIdOrderByComputedAtDesc(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No ranked result yet")
        val assessment=assessments.findById(snapshot.assessmentId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "No ranked result yet") }
        return MyResultResponse(snapshot.rank,snapshot.deliverySequence,utcIsoTimestamp(snapshot.eta),utcIsoTimestamp(snapshot.computedAt),
            assessment.scoreHundredths,assessment.verdict,assessment.comment)
    }

    @GetMapping("/v1/rankings")
    @GetRankingsApi
    fun rankings(@RequestParam(defaultValue="0") offset: Int): List<RankingResponse> {
        if (offset !in 0..10000) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        val latest=snapshots.findFirstByOrderByComputedAtDesc()?.computedAt ?: return emptyList()
        return snapshots.findRankingRows(latest,offset,50).map { RankingResponse(it.nickname,it.rank,it.scoreHundredths,utcIsoTimestamp(it.eta),utcIsoTimestamp(it.computedAt)) }
    }

    @PostMapping("/v1/shares")
    @CreateShareApi
    @Transactional
    fun create(auth: Authentication): CreateShareResponse {
        val userId=(auth.principal as UUID).toString()
        val snapshot=snapshots.findFirstByUserIdOrderByComputedAtDesc(userId)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT,"No ranked result to share")
        val token=shares.findByUserIdAndSnapshotIdAndRevokedAtIsNull(userId,snapshot.id)?.token ?: run {
            val participant=participants.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
            val assessment=assessments.findById(snapshot.assessmentId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
            val value=randomToken()
            shares.save(ShareEntity(value,userId,snapshot.id,participant.nickname,assessment.scoreHundredths,snapshot.rank,snapshot.eta,
                assessment.verdict,assessment.comment,snapshot.computedAt))
            value
        }
        return CreateShareResponse(token,"$base/s/$token","$base/s/$token/image.png")
    }

    @DeleteMapping("/v1/shares/{token}")
    @RevokeShareApi
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@PathVariable token: String, auth: Authentication) {
        val share=shares.findByTokenAndUserId(token,(auth.principal as UUID).toString()) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        shares.delete(share)
    }
    private fun load(token: String): SharePageData {
        if (!token.matches(Regex("[A-Za-z0-9_-]{43}"))) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val share=shares.findByTokenAndRevokedAtIsNull(token) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return SharePageData(share.nickname,share.scoreHundredths,share.rank,share.eta,share.verdict,share.comment,share.computedAt)
    }
    private fun score(row: SharePageData) = java.math.BigDecimal(row.scoreHundredths).movePointLeft(2).setScale(2).toPlainString()
    private fun date(row: SharePageData): String = parseUtcDatabaseTime(row.eta)
        .atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"))

    @GetMapping("/s/{token}",produces=[MediaType.TEXT_HTML_VALUE])
    @PublicSharePageApi
    fun page(@PathVariable token: String): ResponseEntity<String> {
        val row = load(token)
        val title = escapeHtml("${row.nickname} · 산타컷 ${score(row)}점")
        val description = escapeHtml("${row.rank}위 · 선물 도착 예정 ${date(row)} · ${row.verdict}")
        val html = """<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>$title</title><meta property="og:type" content="website"><meta property="og:title" content="$title"><meta property="og:description" content="$description"><meta property="og:url" content="${escapeHtml(base)}/s/$token"><meta property="og:image" content="${escapeHtml(base)}/s/$token/image.png"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630"><meta name="twitter:card" content="summary_large_image"><style>body{font-family:system-ui;background:#fff7ed;color:#432818;max-width:720px;margin:10vh auto;padding:24px}img{width:100%;border-radius:24px}a{color:#b91c1c}</style></head><body><h1>$title</h1><p>$description</p><p>${escapeHtml(row.comment)}</p><img src="/s/$token/image.png" alt="$description"><p>공유 생성 시점의 결과입니다. 순위와 도착 예정일은 변경될 수 있습니다.</p><p>계산 시각: ${escapeHtml(row.computedAt)} UTC</p><a href="${escapeHtml(base)}">나도 산타컷 도전하기</a></body></html>"""
        return ResponseEntity.ok().header("Cache-Control","no-store").header("Referrer-Policy","no-referrer").body(html)
    }

    @GetMapping("/s/{token}/image.png",produces=[MediaType.IMAGE_PNG_VALUE])
    @PublicShareImageApi
    fun image(@PathVariable token: String): ResponseEntity<ByteArray> {
        val row = load(token)
        val image = BufferedImage(1200,630,BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.color = Color(255,247,237); graphics.fillRect(0,0,1200,630)
            graphics.color = Color(185,28,28); graphics.font = Font("Noto Sans CJK KR",Font.BOLD,52)
            graphics.drawString("산타컷 · 너 올해 통과 가능?",60,95)
            graphics.color = Color(67,40,24); graphics.font = Font("Noto Sans CJK KR",Font.BOLD,72)
            graphics.drawString("${row.nickname}  ${score(row)}점",60,220)
            graphics.font = Font("Noto Sans CJK KR",Font.BOLD,40); graphics.drawString("${row.rank}위 · ${row.verdict}".take(40),60,315)
            graphics.font = Font("Noto Sans CJK KR",Font.PLAIN,34); graphics.drawString("선물 도착 예정: ${date(row)}",60,410)
            graphics.font = Font("Noto Sans CJK KR",Font.PLAIN,25); graphics.drawString(row.comment.take(42),60,490)
            graphics.drawString("산타컷 AI 심사 · 재미를 위한 결과",60,575)
        } finally { graphics.dispose() }
        val output = ByteArrayOutputStream(); ImageIO.write(image,"png",output)
        return ResponseEntity.ok().header("Cache-Control","no-store").body(output.toByteArray())
    }
}


