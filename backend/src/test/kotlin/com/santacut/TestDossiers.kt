package com.santacut

import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierMessages
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.global.config.DossierCatalogConfiguration
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * 조서 테스트가 함께 쓰는 사전과 메시지. 테스트는 실제 콘텐츠 문구에 기대지 않도록 작은 픽스처 리소스(`dossier-fixture`)를 쓴다.
 */
object TestDossiers {
    private const val FIXTURE_DIRECTORY = "/dossier-fixture/"
    private val REAL_DIRECTORY: Path = Path.of("src/main/resources/dossier")

    /** 상대 화자 표시(1:1 대화). */
    const val PARTNER = "상대"

    val mapper = jacksonObjectMapper()

    /** 테스트 픽스처 사전. */
    val fixture: DossierCatalog by lazy { readFixture { name -> fixtureText(name) } }

    /** 픽스처 파일 [name]의 내용. */
    fun fixtureText(name: String): String =
        TestDossiers::class.java.getResource(FIXTURE_DIRECTORY + name)?.readText(Charsets.UTF_8) ?: error("픽스처 $name 없음")

    /** 픽스처를 읽되 [name] 파일만 [override]로 바꿔 읽는다. 검사 위반을 만들 때 쓴다. */
    fun readFixture(read: (String) -> String): DossierCatalog = DossierCatalogConfiguration.readCatalog(mapper, read)

    /** 배포할 실제 리소스(src/main/resources/dossier). */
    fun readReal(): DossierCatalog = DossierCatalogConfiguration.readCatalog(mapper) { name -> Files.readString(REAL_DIRECTORY.resolve(name)) }

    fun self(at: LocalDateTime, text: String): ScoringMessage = ScoringMessage(at, text)

    fun partner(at: LocalDateTime, text: String, speaker: String = PARTNER): ScoringMessage = ScoringMessage(at, text, speaker)

    /** 2026-03-02 10:00부터 [minutesApart]분 간격으로 본인 메시지 [texts]를 놓는다. */
    fun selves(texts: List<String>, minutesApart: Long = 60): List<ScoringMessage> =
        texts.mapIndexed { index, text -> self(START.plusMinutes(index * minutesApart), text) }

    fun tally(messages: List<ScoringMessage>, catalog: DossierCatalog = fixture): DossierTally = DossierMessages(messages).tally(catalog)

    val START: LocalDateTime = LocalDateTime.of(2026, 3, 2, 10, 0)
}
