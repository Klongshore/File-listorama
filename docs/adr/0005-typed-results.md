# 5. Typed results instead of exceptions

Status: accepted

`FileSource` calls return `Success`, `PermissionDenied`, `NotFound` or `Io` rather than throwing across layers. Cloud backends extend the set with `Unauthenticated`, `RateLimited` and `Offline`. This gives the UI one error vocabulary for every Source.
