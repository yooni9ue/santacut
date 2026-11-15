import { cp, mkdir } from "node:fs/promises";

// Next.js omits static assets from standalone output; include them for start/Docker.
await mkdir(".next/standalone/.next", { recursive: true });
await cp("public", ".next/standalone/public", { recursive: true });
await cp(".next/static", ".next/standalone/.next/static", { recursive: true });
