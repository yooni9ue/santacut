package com.santacut

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.global.config.RoastCatalogConfiguration
import com.santacut.domain.roast.model.RoastBrief
import com.santacut.domain.roast.model.RoastCatalog
import java.nio.file.Files
import java.nio.file.Path

/**
 * 팩폭 작가 테스트가 함께 쓰는 실제 리소스와 입력 조립 도구. 입력 안전장치와 검증 규칙은 실제 리소스(roast/, dossier/)로 확인한다.
 */
object TestRoasts {
    private val DIRECTORY: Path = Path.of("src/main/resources/roast")

    val mapper = TestDossiers.mapper

    /** 실제 팩폭 작가 리소스. */
    val catalog: RoastCatalog by lazy { RoastCatalogConfiguration.readCatalog(mapper) { name -> Files.readString(DIRECTORY.resolve(name)) } }

    /** 실제 조서 사전. */
    val dossier: DossierCatalog by lazy { TestDossiers.readReal() }

    /** 메시지 [messages]와 그 메시지로 센 조서 집계, 총점 [hundredths], 항목 점수 [dimensions]로 조립한 작가 입력. */
    fun brief(messages: List<ScoringMessage>, hundredths: Int = GIFT_SCORE, dimensions: ScoreComponents = MIXED): RoastBrief =
        RoastBrief.compose(messages, TestDossiers.tally(messages, dossier).stats, GiftVerdict(hundredths), dimensions, catalog, dossier)

    const val GIFT_SCORE = 7373
    const val COAL_SCORE = 4200

    /** 컷(65점) 이상 둘, 아래 둘인 항목 점수. 선물이면 이유 둘·깎인 말 둘, 석탄이면 깎인 말 둘·살아남은 말 하나다. */
    val MIXED: ScoreComponents = ScoreComponents.fromColumn("[80,70,60,50]")
}
