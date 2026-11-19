package com.santacut.domain.dossier.model

/**
 * copy.json 경로별 템플릿 묶음. 앱이 뜰 때 모든 문구를 한 번에 검사하려고 모은다.
 */
class TemplateGroups(private val values: Map<String, Templates>) {
    /** [registered]는 copy.json `slots`에 등록한 슬롯 이름이다. */
    fun violations(registered: Set<String>): List<String> =
        values.flatMap { (path, templates) -> templates.violations(path, registered) }
}
