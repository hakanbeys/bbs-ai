# Contributing to BBS Mod

Thanks for taking an interest in BBS Mod.

This repository is being left open for community continuation, experimentation, and forks. Contributions are welcome, but please treat the project as a production-sensitive codebase with unfinished systems.

## Before You Start

Please keep these goals in mind:

- preserve existing editor and animation behavior unless a change is intentional
- prefer small, understandable patches over huge speculative rewrites
- test runtime behavior, not just compilation
- document anything user-facing or behavior-changing

## Recommended Contribution Flow

1. Fork the repository.
2. Create a focused branch for your work.
3. Keep the scope narrow and well-defined.
4. Test your changes in-game whenever possible.
5. Open a pull request with a clear summary of:
   - what changed
   - why it changed
   - what was tested
   - known limitations or risks

## What Makes a Good Contribution

- bug fixes with a clear reproduction path
- stability improvements
- editor workflow improvements
- cinematic or animation tooling improvements
- AI/runtime diagnostics
- performance fixes with measurable impact
- documentation improvements

## What to Avoid

- untested large refactors
- speculative changes with unclear benefit
- generated code you do not understand
- unrelated formatting-only churn across many files
- shipping personal configs, logs, caches, or build artifacts

## AI-Generated Code

AI-assisted code is allowed, but only if you:

- understand it
- review it
- adapt it to the codebase
- test it properly

Blind copy-paste AI output is not acceptable.

## Build Files

Please avoid changing build and wrapper files unless the PR is specifically about build tooling.

This includes:

- `gradle/`
- `gradlew`
- `gradlew.bat`
- `build.gradle`
- `gradle.properties`

## Code Style

Please follow the existing code style used in the project.

Important conventions:

- opening braces go on the next line
- instance field access and instance method calls should use `this`
- comments must be written in English
- avoid dead code
- avoid unnecessary duplication
- avoid `var`
- prefer explicit, readable code over clever shortcuts

When editing an existing file, match the local style before introducing new patterns.

## Pull Request Notes

A strong PR should include:

- a concise title
- a clear description
- testing notes
- screenshots or logs if the change affects UI or runtime behavior

If the change is large, explain the tradeoffs.

## Forks

If you want to take the project in a very different direction, forking is encouraged.

This repository is intentionally open for people who want to explore their own version of BBS Mod.
