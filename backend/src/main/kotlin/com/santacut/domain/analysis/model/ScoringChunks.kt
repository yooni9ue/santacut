package com.santacut.domain.analysis.model

/**
 * 대화문을 이어진 구간 몇 개로 나눈 목록. 긴 대화의 가운데가 묻히지 않게(lost-in-the-middle) 구간마다 따로 채점한다.
 * 본문이 [CHUNK_CHARS]자 이하면 한 구간이고, 길면 [MAX_CHUNKS]개까지 본문 길이가 비슷하게 나눈다.
 */
class ScoringChunks(private val values: List<ScoringChunk>) {
    /**
     * 모든 호출의 어림 사용량 합. 비용 가드가 호출 전에 한 번에 확인한다.
     * 한 구간이면 [samples]번 묻으므로 그만큼 곱한다.
     */
    fun estimate(samples: Int, estimator: (ScoringChunk) -> ModelUsage): ModelUsage {
        val perRound = values.fold(ModelUsage.NONE) { total, chunk -> total + estimator(chunk) }
        return List(callsPerChunk(samples)) { perRound }.fold(ModelUsage.NONE) { total, usage -> total + usage }
    }

    /**
     * 한 구간이면 같은 구간을 [samples]번 동시에 따로 묻고 항목별 중앙값([ScoreAnswers.median])으로 합친다.
     * 여러 구간이면 구간마다 한 번씩 묻는다([ask]). 구간 호출이 이미 잡음을 평균하기 때문이다. [samples]가 1이면 예전과 같다.
     */
    fun ask(samples: Int, asker: (ScoringChunk) -> ScoreAnswer): ScoringChunkAnswers {
        if (callsPerChunk(samples) == 1) return ask(asker)
        val chunk = values.single()
        return ScoringChunkAnswers(listOf(ScoringChunkAnswer(chunk, ScoreAnswers.sampled(samples) { asker(chunk) }.median())))
    }

    /** 구간 하나를 몇 번 묻는지. 한 구간일 때만 [samples]번이고, 나머지는 한 번이다. */
    private fun callsPerChunk(samples: Int): Int = if (values.size == 1) samples.coerceAtLeast(1) else 1

    /**
     * 구간을 차례로 묻는다. 한 구간이라도 쓸 수 없는 답(제공자 차단, 읽기 실패)이면 남은 구간은 묻지 않는다.
     * 작업은 원자적이라 그 답 하나로 작업 전체가 실패하거나 대기열로 돌아간다.
     */
    fun ask(asker: (ScoringChunk) -> ScoreAnswer): ScoringChunkAnswers {
        val answers = mutableListOf<ScoringChunkAnswer>()
        for (chunk in values) {
            val answer = asker(chunk)
            answers += ScoringChunkAnswer(chunk, answer)
            if (!answer.isUsable()) break
        }
        return ScoringChunkAnswers(answers)
    }

    companion object {
        /** 구간 하나의 본문 글자 수 목표. 한국어 대화문은 약 1.4자가 토큰 하나라 줄 표시를 더해도 약 8~9천 토큰이다. */
        const val CHUNK_CHARS = 10_000

        /** 구간 수 상한. 본문 상한([ScoringDigest.MAX_TRANSCRIPT_CHARS]) 40,000자를 4개로 나눈다. */
        const val MAX_CHUNKS = 4

        /** 앞 구간에서 함께 싣는 줄 수. 구간 첫 줄이 무엇에 답했는지 보이게 한다. */
        const val OVERLAP_TURNS = 4

        fun of(digest: ScoringDigest): ScoringChunks {
            val bounds = digest.turns.partitionBounds(chunkCount(digest.turns.totalLength()))
            return ScoringChunks(bounds.zipWithNext { start, end -> chunkOf(digest, start, end) })
        }

        private fun chunkCount(totalChars: Int): Int =
            ((totalChars + CHUNK_CHARS - 1) / CHUNK_CHARS).coerceIn(1, MAX_CHUNKS)

        private fun chunkOf(digest: ScoringDigest, start: Int, end: Int): ScoringChunk {
            val from = (start - OVERLAP_TURNS).coerceAtLeast(0)
            val omitted = if (start == 0) digest.omittedTurns else 0
            return ScoringChunk(digest.turns.slice(from, end), from + TranscriptTurns.FIRST_NUMBER, start - from, omitted)
        }
    }
}
