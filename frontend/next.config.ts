import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    const backendOrigin = (process.env.BACKEND_ORIGIN ?? "http://127.0.0.1:8080").replace(/\/$/, "");
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
