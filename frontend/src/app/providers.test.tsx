import { useQuery } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import Providers from "./providers";
import { getQueryClient } from "@/lib/query-client";

afterEach(() => getQueryClient().clear());

test("children can fetch and reuse cached data through the app provider", async () => {
  const queryFn = vi.fn().mockResolvedValue("서울");
  function Regions() {
    const { data } = useQuery({ queryKey: ["regions"], queryFn });
    return <p>{data ?? "불러오는 중"}</p>;
  }
  const first = render(<Providers><Regions /></Providers>);
  expect(await screen.findByText("서울")).toBeInTheDocument();
  first.unmount();
  render(<Providers><Regions /></Providers>);
  expect(screen.getByText("서울")).toBeInTheDocument();
  expect(queryFn).toHaveBeenCalledTimes(1);
});
