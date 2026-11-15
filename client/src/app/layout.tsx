import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "산타컷 | SantaCut",
  description: "너 올해 산타컷 통과 가능? AI가 분석하는 나의 산타 점수.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body className="antialiased">{children}</body>
    </html>
  );
}
