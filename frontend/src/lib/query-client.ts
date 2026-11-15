import { isServer, QueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        retry: (failureCount, error) => {
          if (isAxiosError(error) && error.response && error.response.status < 500) {
            return false;
          }
          return failureCount < 1;
        },
      },
      mutations: { retry: false },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  if (isServer) return makeQueryClient();
  // Preserve the browser cache if React retries an initial render after suspending.
  return (browserQueryClient ??= makeQueryClient());
}
