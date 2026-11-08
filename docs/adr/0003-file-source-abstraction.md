# 3. A FileSource abstraction from day one

Status: accepted

Every location sits behind a `FileSource` interface. Only the local backend is built in v1, and the interface starts minimal and read-only with no speculative methods. This makes OneDrive, Google Drive and cross-provider copy and move straightforward later. Source and Account are modelled separately so multiple accounts per provider need no rewrite.
