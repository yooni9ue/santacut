// @vitest-environment node
import { expect, test } from "vitest";
import { getQueryClient } from "./query-client";

test("server renders never share user query caches", () => {
  const first = getQueryClient();
  const second = getQueryClient();
  first.setQueryData(["me"], { nickname: "루돌프" });
  expect(second).not.toBe(first);
  expect(second.getQueryData(["me"])).toBeUndefined();
  first.clear();
  second.clear();
});
