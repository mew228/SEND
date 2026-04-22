# Node Spec Sets

This directory is the shared source of truth for node definitions.

Set layout:
- `primitive/` for engine-native computational building blocks
- `fetch/` for engine-native data access nodes
- `derived/` for graph-defined composite nodes

Conventions:
- one JSON file per node type
- `nodeType` is the canonical identity
- folder names are organizational only
- primitive and fetch specs use the native-spec shape with `executorKey`
- derived specs use the derived-spec shape with `derivedGraph`

Ownership:
- Java will eventually scan these directories, parse the specs, and build the node catalog
- OCaml will eventually load the same specs to construct runtime node definitions
