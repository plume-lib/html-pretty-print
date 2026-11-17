# AGENTS-TODO: file-based end-to-end tests for html-pretty-print

**This is a temporary plan file.  Delete it before any pull request is merged.**

## Goal

Work only in the worktree
`/home/mernst/java/plume-lib/html-pretty-print-fork-mernst-branch-e2e-tests`,
on branch `e2e-tests` (program: plume-lib/html-pretty-print).  Add
end-to-end tests stored as data files, so that they can be run against an
implementation in another language.  Model the format on
`/home/mernst/java/plume-lib/bibtex-clean-fork-mernst-branch-e2e-tests/src/test/resources/end-to-end/README.txt`
and its harness `BibtexCleanEndToEndTest.java` (read only; do not modify that
repository).  Test data goes in `src/test/resources/end-to-end/`, one
subdirectory per test case: `description.txt`, `args.txt`, `input/`,
`stdin.txt`, `goal-out.txt`, `goal-err.txt`, `goal-err-prefix.txt`,
`goal-status.txt`, `goal/`.  The README there documents the format in
language-neutral terms.  A JUnit harness runs the cases; `-Pe2e.command=...`
runs another implementation instead.  Convert `HtmlPrettyPrintTest.java` (it
hard-codes input) into test cases and delete it.  Do not change main source.
Record bugs in goal files and start their descriptions with "BUG:".  Commit
at each milestone (short messages); do not push or open a PR.  The full
`./gradlew build` must pass.  When everything is done, delete this file and
commit the deletion.

## Checklist

- [ ] Harness `src/test/java/org/plumelib/htmlprettyprint/HtmlPrettyPrintEndToEndTest.java`
  (stdin.txt, `{WORKDIR}` substitution in stdout/stderr, CRLF normalization,
  `e2e.command` system property)
- [ ] build.gradle.kts: pass `-Pe2e.command` to the test JVM
- [ ] README.txt in the test-data directory
- [ ] Convert HtmlPrettyPrintTest (well-formed, malformed) and delete it
- [ ] Cases: no-arguments, empty-file, whitespace-only, multiple-files,
  repeated-argument, missing-file, missing-file-among-others, directory-argument
  (BUG), option-like-argument, file-name-with-spaces, non-ASCII file name
- [ ] Cases: pre whitespace (BUG), script whitespace/escaping (BUG), duplicate
  attribute (BUG), default attributes (BUG), unquoted/boolean attributes,
  uppercase tags, entities, comments/doctype/PI/CDATA, long-line wrapping,
  deep nesting, tables, text outside html, unknown elements
- [ ] Cases: UTF-8 non-ASCII, meta charset Latin-1, BOM, CRLF and CR line
  endings, tabs, control characters, invalid attribute names, namespace prefixes
- [ ] stdin-ignored case
- [ ] Run `./gradlew spotlessApply build`; markdownlint this file
