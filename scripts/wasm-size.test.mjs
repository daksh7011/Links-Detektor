import { test, after } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";
import { main, measure } from "./wasm-size.mjs";

const dir = mkdtempSync(join(tmpdir(), "wasm-size-"));
mkdirSync(join(dir, "sub"));
writeFileSync(join(dir, "a.txt"), "a".repeat(10_000));
writeFileSync(join(dir, "sub", "b.bin"), randomBytes(4_096));
writeFileSync(join(dir, "app.js.map"), "m".repeat(5_000));
writeFileSync(join(dir, "app.js.LICENSE.txt"), "license");

after(() => rmSync(dir, { recursive: true, force: true }));

test("measure sums raw and compressed sizes per file, leaving out source maps and licence files", () => {
  const { files, total } = measure(dir);
  assert.deepEqual(files.map((f) => f.path).sort(), ["a.txt", "sub/b.bin"]);
  assert.equal(total.raw, 14_096);
  assert.ok(total.brotli < total.raw && total.gzip < total.raw);
  assert.ok(files.find((f) => f.path === "a.txt").brotli < 200);
});

function quietly(args) {
  const log = console.log;
  const error = console.error;
  console.log = () => {};
  console.error = () => {};
  try {
    return main(args);
  } finally {
    console.log = log;
    console.error = error;
  }
}

test("main exits 0 within the budget and 1 over it", () => {
  assert.equal(quietly([dir]), 0);
  assert.equal(quietly([dir, "--budget-mb", "0.0001"]), 1);
  assert.equal(quietly(["--budget-mb", "9", dir]), 0);
});

test("main exits 2 for a missing directory or a bad budget", () => {
  assert.equal(quietly([join(dir, "missing")]), 2);
  assert.equal(quietly([]), 2);
  assert.equal(quietly([dir, "--budget-mb", "x"]), 2);
  assert.equal(quietly([dir, "--budget-mb", ""]), 2);
  assert.equal(quietly([dir, "--budget-mb", "-1"]), 2);
});
