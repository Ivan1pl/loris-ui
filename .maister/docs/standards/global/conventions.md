## Development Conventions

### Predictable Structure
Organize files and directories in a logical, navigable layout.

### Up-to-Date Documentation
Keep README files current with setup steps, architecture overview, and contribution guidelines.

### Standards Are Rules; the Index Points
A standard states what new and changed code must do, with a one-line reason where useful and at most a representative example class; it does not describe how particular classes work — that belongs in the code's Javadoc and comments, and architecture belongs in `project/architecture.md`. Project docs hold no checklists (manual verification procedures live in `project/manual-testing.md`). `INDEX.md` entries are short pointers saying what a file covers and when to read it, never summaries of its content.

### Clean Version Control
Write clear commit messages, use feature branches, and add meaningful descriptions to pull requests.

### Environment Variables
Store configuration in environment variables; never commit secrets or API keys.

### Minimal Dependencies
Keep dependencies lean and up-to-date; document why major ones are included.

### Consistent Reviews
Follow a defined code review process with clear expectations for reviewers and authors.

### Testing Standards
Define required test coverage (unit, integration, etc.) before merging.

### Feature Flags
Use flags for incomplete features instead of long-lived branches.

### Changelog Updates
Maintain a changelog or release notes for significant changes.

### Build What's Needed
Avoid speculative code and "just in case" additions (see minimal-implementation.md).

### Lightweight Process (Solo Maintainer)
The project has a single maintainer: keep process, tooling and CI lightweight. Prefer minimal, low-ceremony solutions (e.g. a single `./gradlew build` CI job) over heavyweight pipelines, gates or processes.

### Build and IDE Outputs Not Committed
Gradle outputs (`build/`, `.gradle`), the `.idea/` directory and other IDE files (`*.iml`, `out/`, `.vscode/`, Eclipse/NetBeans files) are git-ignored; only `gradle-wrapper.jar` is kept. Do not commit IDE settings or build artifacts.
