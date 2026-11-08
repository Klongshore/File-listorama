# File-listorama: domain glossary

Shared vocabulary for people and agents. Use these terms in code, tickets and docs.

**Source**
A place files live: the device's local storage today, a OneDrive or Google Drive account later. Every Source is accessed through the `FileSource` interface, so the UI treats them all alike.

**Account**
A signed-in identity for a Source. Local storage has a trivial built-in account. Cloud Sources can have several Accounts each (for example personal and work OneDrive). Source and Account are separate concepts from day one.

**FileRef**
An opaque identity for one item: a source id plus an item id. It is never a path. Paths are for display only, because Google Drive allows duplicate names in a folder and files with multiple parents.

**Entry**
A listed item: name, size, modified time, kind (file or folder), a type hint, a hidden flag, and a hint saying whether a thumbnail is available.

**FileSource**
The interface every Source implements. v1 is read-only: list a folder's children, get an Entry's metadata, open or stream content, and provide a thumbnail. Write operations arrive later.

**Typed result**
The outcome of a `FileSource` call: `Success`, `PermissionDenied`, `NotFound` or `Io`. Cloud backends add `Unauthenticated`, `RateLimited` and `Offline`. Exceptions never cross layer boundaries.

**Location**
A top-level root the user can pick from the navigation drawer, such as internal storage or the SD card. Each Location belongs to a Source.
