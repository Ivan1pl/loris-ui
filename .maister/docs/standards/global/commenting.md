## Commenting

### Let Code Speak
Write code that explains itself through structure and naming.

### Comment Sparingly
Add brief comments only when the logic isn't self-evident from the code.

### No Change Comments
Avoid comments about recent fixes or changes; comments should be timeless explanations, not changelogs.

### Javadoc on Exported API
Public types and public/protected members in exported packages — especially in `loris-ui-api` — require Javadoc describing the contract, null semantics (which arguments/returns may be null) and threading expectations where relevant. Internal (`impl`) and private code needs no Javadoc; use sparse `//` comments only to explain non-obvious "why". Existing code currently lacks Javadoc: add it when touching public API (roadmap Phase 2).
