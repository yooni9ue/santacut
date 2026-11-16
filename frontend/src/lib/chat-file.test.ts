import { expect, test } from "vitest";
import { extractSpeakers, MAX_CHAT_BYTES, validateChatFile } from "./chat-file";
const bytes = (text: string) => new TextEncoder().encode(text);

test.each([
  ["[나] [오전 9:01] 안녕\r\n[친구] [오후 1:03] 고마워", ["나", "친구"]],
  ["2026. 11. 16. 오전 9:01, 나 : 안녕\n2026. 11. 16. 13:03, 친구 : 고마워", ["나", "친구"]],
  ["2026년 11월 16일 오전 9:01, 나 : 안녕\n2026년 11월 16일 오후 1:03, 친구 : 고마워", ["나", "친구"]],
])("extracts names from supported Kakao exports", (text, expected) => {
  expect(extractSpeakers(bytes(`\uFEFF${text}`), "KAKAO")).toEqual(expected);
});
test("does not treat continuation lines in another export format as new speakers", () => {
  expect(extractSpeakers(bytes("[나] [오전 9:01] 안녕\n2026. 11. 16. 13:03, 가짜 : 본문\n[나] [오후 1:03] 또 안녕"), "KAKAO")).toEqual(["나"]);
});
test("normalizes duplicates but preserves the selected name for transmission", () => {
  expect(extractSpeakers(bytes("[ 나 ] [오전 9:01] 안녕\n[나] [오후 1:03] 안녕"), "KAKAO")).toEqual([" 나 "]);
});
test("extracts Instagram sender names including UTF-8 encoded as Latin-1", () => {
  const encoded = String.fromCharCode(...bytes("산타"));
  expect(extractSpeakers(bytes(JSON.stringify({ messages: [{ sender_name: encoded }, { sender_name: "루돌프" }] })), "INSTAGRAM")).toEqual(["산타", "루돌프"]);
});
test("rejects malformed, binary, non-UTF8 and empty files", () => {
  for (const value of [new Uint8Array(), new Uint8Array([0xff, 0xfe]), bytes("PK archive"), bytes("hello\0world"), bytes("시스템 알림")]) {
    expect(() => extractSpeakers(value, "KAKAO")).toThrow();
  }
  expect(() => extractSpeakers(bytes('{"messages":[]}'), "INSTAGRAM")).toThrow();
  expect(() => extractSpeakers(bytes('{"messages":null}'), "INSTAGRAM")).toThrow();
});
test("checks source-specific file types and 20MB boundary before reading", () => {
  expect(() => validateChatFile({ name: "대화.TXT", size: MAX_CHAT_BYTES }, "KAKAO")).not.toThrow();
  expect(() => validateChatFile({ name: "대화.txt", size: MAX_CHAT_BYTES + 1 }, "KAKAO")).toThrow("20MB");
  expect(() => validateChatFile({ name: "대화.json", size: 1 }, "KAKAO")).toThrow();
});
