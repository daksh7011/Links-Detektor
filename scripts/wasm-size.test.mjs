import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";
import { measure } from "./wasm-size.mjs";

test("measure sums raw and compressed sizes per file", () => {
  const dir = mkdtempSync(join(tmpdir(), "wasm-size-"));
  mkdirSync(join(dir, "sub"));
  writeFileSync(join(dir, "a.txt"), "a".repeat(10_000));
  writeFileSync(join(dir, "sub", "b.bin"), randomBytes(4_096));
  const { files, total } = measure(dir);
  assert.deepEqual(files.map((f) => f.path).sort(), ["a.txt", "sub/b.bin"]);
  assert.equal(total.raw, 14_096);
  assert.ok(total.brotli < total.raw && total.gzip < total.raw);
  assert.ok(files.find((f) => f.path === "a.txt").brotli < 200);
});
