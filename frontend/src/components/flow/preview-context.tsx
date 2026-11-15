"use client";

import { createContext, useContext, useState } from "react";

export type Source = "KAKAO" | "INSTAGRAM";
interface PreviewData {
  source: Source;
  fileName: string;
  speaker: string;
  nickname: string;
  province: string;
  district: string;
}
const initial: PreviewData = {
  source: "KAKAO", fileName: "", speaker: "나", nickname: "", province: "서울", district: "강남구",
};
const PreviewContext = createContext<{
  data: PreviewData;
  update: (patch: Partial<PreviewData>) => void;
} | null>(null);

export function PreviewProvider({ children }: { children: React.ReactNode }) {
  const [data, setData] = useState(initial);
  return <PreviewContext.Provider value={{ data, update: (patch) => setData((previous) => ({ ...previous, ...patch })) }}>{children}</PreviewContext.Provider>;
}

export function usePreview() {
  const context = useContext(PreviewContext);
  if (!context) throw new Error("PreviewProvider가 필요합니다.");
  return context;
}

export const sourceName = (source: Source) => source === "KAKAO" ? "카카오톡" : "인스타그램";
