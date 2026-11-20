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
     * 반복 호출을 어떻게 돌릴지는 [sampler]가, 여러 구간을 어떻게 돌릴지(차례로, 동시에)는 [spreader]가 정한다.
     */
    fun ask(
        samples: Int,
        sampler: (Int, () -> ScoreAnswer) -> List<ScoreAnswer>,
        spreader: (List<ScoringChunk>, (ScoringChunk) -> ScoreAnswer) -> List<ScoreAnswer>,
        asker: (ScoringChunk) -> ScoreAnswer,
    ): ScoringChunkAnswers {
        if (callsPerChunk(samples) == 1) return ask(spreader, asker)
        val chunk = values.single()
        val answer = ScoreAnswers(sampler(samples) { asker(chunk) }).median()
        return ScoringChunkAnswers(listOf(ScoringChunkAnswer(chunk, answer)))
    }

    /** 구간 하나를 몇 번 묻는지. 한 구간일 때만 [samples]번이고, 나머지는 한 번이다. */
    private fun callsPerChunk(samples: Int): Int = if (values.size == 1) samples.coerceAtLeast(1) else 1

    /**
     * 구간을 [spreader]로 묻고 답을 구간 순서대로 싣는다. 한 구간이라도 쓸 수 없는 답(제공자 차단, 읽기 실패)이면
     * 합칠 때([ScoringChunkAnswers.toScore]) 구간 순서로 처음 나온 그 답 때문에 작업 전체가 실패하거나 대기열로 돌아간다(원자적).
     * [spreader]는 앞 답이 쓸 수 없으면 뒤 구간을 부르지 않고 답을 덜 돌려줄 수 있다. 그때는 돌려준 답까지만 싣는다.
     */
    fun ask(spreader: (List<ScoringChunk>, (ScoringChunk) -> ScoreAnswer) -> List<ScoreAnswer>, asker: (ScoringChunk) -> ScoreAnswer): ScoringChunkAnswers =
        ScoringChunkAnswers(values.zip(spreader(values, asker)) { chunk, answer -> ScoringChunkAnswer(chunk, answer) })

    companion object {
        /** 구간 하나의 본문 글자 수 목표. 한국어 대화문은 약 1.4자가 토큰 하나라 줄 표시와 시스템 프롬프트를 더해도 약 7천 토큰이다(§14.13). */
        const val CHUNK_CHARS = 6_700

        /** 구간 수 상한. 본문 상한([ScoringDigest.MAX_TRANSCRIPT_CHARS]) 52,000자를 구간 길이([CHUNK_CHARS])에 가깝게 8개로 나눈다. */
        const val MAX_CHUNKS = 8

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
