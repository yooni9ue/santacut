package com.santacut.domain.prompt.model

import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Check
import java.time.Instant
import java.util.UUID

/**
 * AI에 보내는 시스템 프롬프트의 한 판. 같은 이름의 판은 1부터 1씩 늘고, 한 번 저장한 판은 바꾸지 않는다.
 * 새 내용은 [next]로 다음 판을 만들어 더한다. AI 호출은 이름별 가장 높은 판을 쓴다.
 *
 * @property name 프롬프트 이름(`score` 등). 규칙은 [PromptName]이 지킨다.
 * @property version 같은 이름 안의 판 번호
 */
@Entity
@Table(name = "prompts")
@Check(constraints = "version >= 1")
class Prompt(
    id: String,
    name: String,
    version: Int,
    content: String,
    createdAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "version", nullable = false)
    var version: Int = version
        protected set

    @Column(name = "content", nullable = false)
    var content: String = content
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false, columnDefinition = CREATED_AT_COLUMN_DEFINITION)
    var createdAt: Instant = createdAt
        protected set

    /** 이 판 다음 판으로 [content]를 담는다. */
    fun next(content: String, createdAt: Instant): Prompt =
        Prompt(UUID.randomUUID().toString(), name, version + 1, content, createdAt)

    companion object {
        /** 이름의 첫 판 번호. */
        const val FIRST_VERSION = 1

        /** prompts.sql은 created_at 없이 넣으므로 DB가 지금 시각(UTC)을 채운다. */
        private const val CREATED_AT_COLUMN_DEFINITION = "TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now'))"

        /** [name]의 첫 판을 만든다. */
        fun initial(name: PromptName, content: String, createdAt: Instant): Prompt =
            Prompt(UUID.randomUUID().toString(), name.value, FIRST_VERSION, content, createdAt)
    }
}
