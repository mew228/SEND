# Graph Layer

This directory defines the OCaml graph model used by the execution engine.

Responsibilities:
- represent typed node specs, node instances, explicit port-to-port edges, and node config data
- carry typed runtime port values as graph state
- validate graph structure and source/target port compatibility
- provide stateful graph access plus traversal helpers for DAG-style execution

Planned module layout:
- `node_id.ml` / `node_id.mli`: stable node identifiers plus ordered maps/sets
- `node.ml` / `node.mli`: graph data types, typed values, port specs, data field specs, node specs, node instances, and edges
- `graph_error.ml` / `graph_error.mli`: graph validation and state errors
- `graph.ml` / `graph.mli`: stateful graph storage, validation, state management, and traversal helpers
- `tests/`: hand-built graph checks for the initial model
- `utop_load.ml`: toplevel loader for interactive experimentation and tests

Non-goals for this layer:
- node execution logic and executor lookup
- JSON decoding
- Java bridge handling
- full node-specific semantic validation

Using this directory in `utop`:
- `#use "./ocaml-engine/graph/utop_load.ml";;`
- `Graph_test.run_all ();;`
