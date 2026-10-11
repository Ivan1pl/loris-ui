## Coding Style

### Naming Consistency
Follow established naming patterns for variables, functions, classes, and files throughout the project.

### Automated Style Checks
Consistent indentation, spacing, line breaks, imports and naming are enforced by a Checkstyle build gate (`config/checkstyle/`; any violation fails `./gradlew build`) plus a shared `.editorconfig`. There is no auto-formatter: write code in the documented style (`java/formatting.md`, `java/naming.md`) and fix what Checkstyle reports instead of suppressing it.

### Descriptive Names
Choose names that clearly communicate intent; avoid cryptic abbreviations or single-letter identifiers outside tight loops.

### Focused Functions
Write functions that do one thing well; smaller functions are easier to read, test, and maintain.

### Uniform Indentation
Spaces only (4 per level), set in `.editorconfig` and enforced by Checkstyle (no tab characters, no trailing whitespace, final newline).

### No Dead Code
Remove unused imports, commented-out blocks, and orphaned functions instead of leaving them behind. Checkstyle rejects unused and redundant imports.

### No Backward Compatibility Unless Required
Avoid extra code paths for backward compatibility unless explicitly needed.

### DRY (Don't Repeat Yourself)
Extract repeated logic into reusable functions or modules.
