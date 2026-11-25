# Token efficiency

How to spend fewer tokens without losing quality. Rule 21 of [AGENTS.md](../AGENTS.md) sends you here; the reasons
are in D-045. **Quality wins:** if a practice here would skip a rule of AGENTS.md (the cycle, the
approval, the tests of rule 17, the coverage of D-033, the evidence of step 7), follow AGENTS.md and drop the practice.

Every call re-reads the whole conversation. In our logs (wiki *Token usage*, 17/11/2026) each call re-read about
370,000 tokens, and that re-reading is 97 % of all our tokens. A file read early in a long conversation is paid again
in every later call.

## For every agent
1. **One task per conversation.** One step of the plan, one document (minutes, wiki page, decision) or one review.
   When the step is committed and the plan updated (steps 5 and 8 of the cycle), start a new conversation that reads
   AGENTS.md and the plan. *Why:* the context grows with every call. *Saves:* most of the cached input. *Risk:*
   anything agreed only in the chat is lost, so write it in the plan or the issue first.
2. **Decisions: only the ones cited.** List them with `grep -n "^## D-0" DECISIONS.md` (about 700 tokens) and read
   only the entries you need (about 400 each). Read the whole file (about 18,600) only to work on DECISIONS.md itself.
3. **Plans: *Status* first.** Read *Status* (50–160 tokens), then the step you will do and the questions it cites.
   Read the whole plan (up to 5,000) when you write it or re-plan.
4. **Read once, search first.** Do not read a file again in the same conversation unless it changed. Search
   (`grep -n`, the search tool) before opening a large file, and open only the part you need.
5. **Diagrams: only your side.** `doc/design/Cn-server-design.png` or `Cn-client-design.png`: each image costs
   700–4,800 tokens.
6. **Build: filter the output, never hide it.** `mvn -B verify` prints about 16,500 tokens when it is green. Use:
   - bash: `mvn -B -ntp verify 2>&1 | grep -E "Tests run:|FAIL|ERROR|WARN|BUILD|coverage checks" | grep -v " -- in "`
   - PowerShell (tested on Windows, 17/11/2026): `mvn -B -ntp verify 2>&1 | ForEach-Object { "$_" } | Select-String -CaseSensitive 'Tests run:|FAIL|ERROR|WARN|BUILD|coverage checks' | Select-String -NotMatch ' -- in |Building jar'; "Exit code: $LASTEXITCODE"`
     (`ForEach-Object` turns the lines that Maven writes to stderr into text; `-CaseSensitive` is needed because
     `Select-String` ignores case and would keep lines such as `Building jar`). The build is green only if it prints
     `BUILD SUCCESS` and `Exit code: 0`.

   It keeps the totals per module, each failure with its message and line, compile errors and the coverage rules
   (250–370 tokens). The pipe hides Maven's exit code: the build is green only if `BUILD SUCCESS` is printed. Never
   `-q`: it hides which coverage rule failed and the test counts. To prove that a new test class ran (E-14), find it
   in `<module>/target/surefire-reports/`; for the detail of one failure, read `<module>/target/surefire-reports/<Class>.txt`.
   While you work you may build one module (`-pl <module> -am`); before each commit, the full build (step 5).
7. **Coverage: the CSV first.** `<module>/target/site/jacoco/jacoco.csv` (200–600 tokens) gives the lines and
   branches missed per class. Then open the HTML page of that class only. Never the XML (10,000 or more).
8. **Git: short forms first.** `git status --short`, `git log --oneline -n 10` (250 tokens instead of 1,000),
   `git diff --stat` (350 instead of 6,700 for ten commits), then the diff of one file at a time.
9. **GitHub:** read the comments of your sub-issue (they hold the answers), but only section 6 of the component
   issue. For a red CI, `gh run view <id> --log-failed`, not `--log`.
10. **Long output to a file.** If a command can print more than a screen, write it to a file outside the repository
    and read only the part you need.
11. **Documents and reviews.** One document per conversation. Read the template or the last similar page, not all
    of them. For a review, `git diff --stat` first and then file by file.
12. **Be specific.** Name the files, classes and step in the prompt: a vague prompt makes the agent scan.

## Claude Code only
13. **`/clear` at the hand-over of practice 1** (`/rename` first, to find it with `/resume`). It costs nothing. Do
    not resume a conversation of days: after a break longer than the cache lifetime (1 hour on Pro or Max within the
    plan, 5 minutes with the API or usage credits) the first call reads everything again at the full price.
14. **`/compact` only at a natural break**, saying what to keep: `/compact Keep the sub-issue, the plan file, the
    current step, the decisions cited and the failing tests`. It reads the whole conversation once and the summary
    loses detail, so prefer practice 13 when the plan is up to date. To drop a wrong path, `/rewind` (Esc Esc)
    instead of correcting on top of it.
15. **Model and effort at the start, not in the middle.** Each model has its own cache, so a switch reads everything
    again (`opusplan` switches at every toggle of plan mode). The default (Opus 5.5, effort `medium`) for reading,
    asking, planning, tests and diagnosis; `high` for a bug or a failing test you do not understand. Sonnet only for
    mechanical steps of an approved plan and only if the member chooses it: it is cheaper, but its quality has not
    been compared here yet.
16. **Subagents for noisy work:** the whole test suite, a search over many files, a long log. Only the summary comes
    back. Rules:
    - The prompt of every subagent repeats the rules of AGENTS.md that apply to it: read only when it explores; never
      invent an expected result and never change an expected value (rule 17); cite the file and line of every fact
      (rule 12); report only what was asked.
    - *Explore* and *Plan* do not load CLAUDE.md, so they do not know AGENTS.md unless the prompt says it.
    - Not for small tasks: each subagent spends its own tokens and does not see our conversation.
17. **Check while you work.** `/usage` shows the tokens of the session by model (input, output, cache read, cache
    write) and a *Prompt cache* line; `/context` shows what fills the context. `/clear` resets the totals.

## Counting tokens (rule 19)
Say in the row how it was counted: *exact* (from the tool) or *estimated* (and how).

| Tool | Where to read it |
|---|---|
| Claude Code | The session logs of the tool, as the rows already on the page: *new input* = input + cache creation, *cached input* = cache read. `/usage` for the current session. The log format is internal and can change: write the Claude Code version. Optional, if the member wants it (rule 6): OpenTelemetry, metric `claude_code.token.usage`. |
| Claude Pro, Max, Team | `/usage` also shows the plan bars (% of the limits). Its dollar figure is not a bill. |
| Claude API | The Usage page of the Claude Console is the real figure. |
| GitHub Copilot | Premium requests: github.com → *Your Copilot* → *Usage*, or the Copilot icon in the VS Code status bar. AI credits: hover over a response (one turn), the context window control (the session) and the *Agent Debug Logs* (total tokens). Which one our licence uses is not known yet. |
| Claude Cowork | No documented count per task was found (17/11/2026): write *unknown* or estimate. |
| Tools without logs | Estimate and say how, for example the characters sent and received divided by 4. |

Dollar figures are the equivalent price of the API, only to compare: AI costs 0 € to the project (assumption A3).
