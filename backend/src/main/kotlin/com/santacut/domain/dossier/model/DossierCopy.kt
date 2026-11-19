package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 조서 문구 템플릿과 칭호 부품(copy.json). 슬롯 문법과 금지 규칙은 [Templates.violations]가 검사한다.
 *
 * @property slots 템플릿이 쓸 수 있는 슬롯 목록(미등록 슬롯 검사의 허용 목록)
 * @property notes 작가가 엔진 쪽에 남긴 메모. 엔진은 읽지 않는다.
 * @property speakers 화자 key와 화면 이름
 * @property lowData 본인 메시지가 적을 때 맨 앞에 붙이는 줄
 * @property reupload 이 기능 전 결과에 보여 줄 재업로드 안내. 서버는 검사만 하고 프론트가 같은 뜻의 안내를 띄운다.
 */
data class DossierCopy(
    val version: Int,
    val slots: SlotSpecs,
    val notes: List<String> = emptyList(),
    val speakers: Map<String, String>,
    val teaser: Templates,
    val hook: HeadlineMemoCopy,
    @JsonProperty("hook_fallback") val hookFallback: HeadlineMemoCopy,
    val ledger: LedgerCopy,
    val extremes: ExtremeCopies,
    val acts: ActsCopy,
    val dossier: CaseFileCopy,
    val verdict: VerdictCopy,
    val summons: Templates,
    @JsonProperty("low_data") val lowData: Templates,
    val reupload: Templates,
    val mild: MildCopy,
    val titles: TitleCopy,
) {
    fun violations(): List<String> =
        TemplateGroups(fixedGroups() + keyedGroups() + extremes.groups()).violations(slots.names()) +
            slots.violations() + extremes.speakerViolations(speakers) +
            dossier.summaryByBand.missing(VerdictBand.KEYS, "dossier.summary_by_band") +
            verdict.reasonByDimension.missingDimensions("verdict.reason_by_dimension")

    private fun fixedGroups(): Map<String, Templates> = mapOf(
        "teaser" to teaser, "hook.headline" to hook.headline, "hook.memo" to hook.memo,
        "hook_fallback.headline" to hookFallback.headline, "hook_fallback.memo" to hookFallback.memo,
        "ledger.headline" to ledger.headline, "ledger.bleep" to ledger.bleep,
        "acts.headline" to acts.headline, "acts.line" to acts.line, "acts.empty" to acts.empty,
        "dossier.header" to dossier.header, "verdict.low_band_title" to verdict.lowBandTitle,
        "summons" to summons, "low_data" to lowData, "reupload" to reupload,
        "mild.extremes_memo" to mild.extremesMemo, "mild.summary" to mild.summary,
        "mild.hook_memo" to mild.hookMemo, "mild.summons" to mild.summons,
        "titles.default_modifier" to titles.defaultModifier, "titles.default_role" to titles.defaultRole,
    )

    private fun keyedGroups(): Map<String, Templates> =
        ledger.memoByCategory.prefixed("ledger.memo_by_category") + dossier.charges.prefixed("dossier.charges") +
            dossier.merits.prefixed("dossier.merits") + dossier.summaryByBand.prefixed("dossier.summary_by_band") +
            verdict.reasonByDimension.prefixed("verdict.reason_by_dimension") + verdict.note.prefixed("verdict.note") +
            titles.modifierByMetric.prefixed("titles.modifier_by_metric") + titles.roleByMetric.prefixed("titles.role_by_metric")
}
