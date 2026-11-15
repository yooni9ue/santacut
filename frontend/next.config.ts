import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const configuredOrigin = process.env.BACKEND_ORIGIN;
    if (process.env.VERCEL && !configuredOrigin) {
      throw new Error("Vercel 배포에는 BACKEND_ORIGIN을 설정해야 합니다.");
    }
    const origin = new URL(configuredOrigin ?? "http://127.0.0.1:8080");
    if (!["http:", "https:"].includes(origin.protocol) || origin.username || origin.password || origin.pathname !== "/" || origin.search || origin.hash) {
      throw new Error("BACKEND_ORIGIN에는 경로와 인증 정보가 없는 서버 origin을 설정하세요.");
    }
    if (process.env.VERCEL && origin.protocol !== "https:") {
      throw new Error("Vercel의 BACKEND_ORIGIN은 HTTPS 주소여야 합니다.");
    }
    const backendOrigin = origin.origin;
    return ["/v1/:path*", "/s/:path*", "/swagger-ui/:path*", "/swagger-ui.html", "/v3/:path*"].map((source) => ({
      source,
      destination: `${backendOrigin}${source}`,
    }));
  },
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
