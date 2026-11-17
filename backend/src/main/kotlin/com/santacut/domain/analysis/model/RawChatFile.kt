package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.Sha256
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * 참가자가 올린 대화 내보내기 원본 파일. 파일 이름, 확장자, Content-Type은 믿지 않고 내용만 본다.
 */
class RawChatFile(
    val source: DataSource,
    private val content: ByteArray,
) {
    /** 저장할 원본 바이트. 최대 20MB라 복사하지 않고 그대로 넘긴다. 받는 쪽은 내용을 바꾸지 않는다. */
    fun content(): ByteArray = content

    /**
     * 받기 전에 명백히 대화 텍스트가 아닌 파일을 거른다. 빈 파일, zip 압축 파일, 앞부분에 NUL 바이트가 있는 바이너리 파일은
     * [ErrorCode.UNSUPPORTED_FILE_FORMAT]이다. 형식을 끝까지 확인하는 일은 검증 작업자가 한다.
     */
    fun verifyReceivable() {
        if (content.isEmpty() || isZipArchive() || hasNulInHead()) {
            throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        }
    }

    /**
     * 같은 Idempotency-Key로 다시 보낸 요청이 같은 요청인지 가리는 해시.
     * 출처, 본인 이름, 파일 내용을 NUL 바이트로 나눠 이어 붙인 값의 SHA-256이다.
     * 큰 파일 내용은 이어 붙이지 않고 해시에 바로 흘려 넣어 복사본을 만들지 않는다.
     */
    fun requestHash(selfSpeaker: String): String =
        Sha256.hex(source.name.toByteArray() + SEPARATOR + selfSpeaker.toByteArray() + SEPARATOR, content)

    /**
     * 엄격한 UTF-8로 읽고 맨 앞 BOM을 뗀다. 올바른 UTF-8이 아니면(EUC-KR 등) [ErrorCode.UNSUPPORTED_FILE_FORMAT]이다.
     */
    fun decodeText(): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(content)).toString()
        } catch (exception: CharacterCodingException) {
            throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        }
        return text.removePrefix(BYTE_ORDER_MARK)
    }

    /** 엄격한 UTF-8로 읽은 텍스트를 출처([source])에 맞는 파서로 메시지 목록으로 바꾼다. */
    fun readMessages(readInstagramThread: (String) -> InstagramThread): ChatMessages =
        source.readMessages(decodeText(), readInstagramThread)

    private fun isZipArchive(): Boolean =
        content.size >= ZIP_MAGIC.size && content.copyOf(ZIP_MAGIC.size).contentEquals(ZIP_MAGIC)

    private fun hasNulInHead(): Boolean = content.copyOf(minOf(content.size, BINARY_CHECK_LENGTH)).contains(NUL)

    companion object {
        /** zip 파일 첫 4바이트(PK\u0003\u0004). */
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

        /** 바이너리 파일인지 보려고 NUL 바이트를 찾는 앞부분 길이(바이트). */
        private const val BINARY_CHECK_LENGTH = 4096

        private const val BYTE_ORDER_MARK = "\uFEFF"
        private const val NUL: Byte = 0
        private val SEPARATOR = byteArrayOf(NUL)
    }
}
