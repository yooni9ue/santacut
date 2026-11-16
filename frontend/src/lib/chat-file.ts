import type { Source } from "@/components/flow/preview-context";

export const MAX_CHAT_BYTES = 20 * 1024 * 1024;
const pc = /^\[(.{1,100}?)\] \[(오전|오후) \d{1,2}:\d{2}\] .*$/;
const ios = /^\d{4}\. \d{1,2}\. \d{1,2}\. (?:(?:오전|오후) )?\d{1,2}:\d{2}, (.{1,100}?) : .*$/;
const android = /^\d{4}년 \d{1,2}월 \d{1,2}일 (?:오전|오후) \d{1,2}:\d{2}, (.{1,100}?) : .*$/;
const formatError = () => new Error("지원하는 UTF-8 대화 파일을 선택해 주세요. 카카오톡은 TXT, 인스타그램은 압축을 푼 message_1.json을 사용해요.");

function instagramName(value: string) {
  if ([...value].some(character => character.codePointAt(0)! > 255)) return value;
  try { return new TextDecoder("utf-8", { fatal: true }).decode(Uint8Array.from(value, character => character.charCodeAt(0))); }
  catch { return value; }
}

// Only speaker names leave this function. Message text stays in browser memory.
export function extractSpeakers(bytes: Uint8Array, source: Source): string[] {
  if (!bytes.length || bytes.length > MAX_CHAT_BYTES) throw new Error("내용이 있는 20MB 이하 파일을 선택해 주세요.");
  let text: string;
  try { text = new TextDecoder("utf-8", { fatal: true }).decode(bytes).replace(/^\uFEFF/, ""); }
  catch { throw formatError(); }
  if (text.includes("\0") || (bytes[0] === 0x50 && bytes[1] === 0x4b)) throw formatError();
  const names = new Map<string, string>();
  function add(value: string) {
    const normalized = value.trim().normalize("NFC");
    if (normalized.length && normalized.length <= 100 && !names.has(normalized)) names.set(normalized, value);
  }
  if (source === "INSTAGRAM") {
    let document: unknown;
    try { document = JSON.parse(text); } catch { throw formatError(); }
    if (!document || typeof document !== "object" || !("messages" in document) || !Array.isArray(document.messages)) throw formatError();
    for (const message of document.messages) {
      if (message && typeof message === "object" && typeof message.sender_name === "string") add(instagramName(message.sender_name));
    }
  } else {
    let format: RegExp | undefined;
    for (const line of text.split(/\r?\n/)) {
      format ??= [pc, ios, android].find(expression => expression.test(line));
      const match = format?.exec(line);
      if (match) add(match[1]);
    }
  }
  if (!names.size) throw new Error("대화에서 화자를 찾지 못했어요. 지원하는 내보내기 원본 파일인지 확인해 주세요.");
  return [...names.values()];
}

export function validateChatFile(file: Pick<File, "size" | "name">, source: Source) {
  if (!file.size || file.size > MAX_CHAT_BYTES) throw new Error("내용이 있는 20MB 이하 파일을 선택해 주세요.");
  if (!file.name.toLowerCase().endsWith(source === "KAKAO" ? ".txt" : ".json")) throw formatError();
}
