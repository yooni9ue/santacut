import type { Metadata, Viewport } from "next";
import Providers from "./providers";
import { GoogleAnalytics } from "./analytics";
import "./globals.css";
import "./chaos.css";
import "./sketch.css";
import "./toon.css";

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: "#f7f4e9",
};

export const metadata: Metadata = {
  title: "산타컷 | SantaCut",
  description: "너 올해 산타컷 통과 가능? AI가 분석하는 나의 산타 점수.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body className="antialiased"><Providers>{children}</Providers><GoogleAnalytics /></body>
    </html>
  );
}
