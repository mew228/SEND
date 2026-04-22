# Executors Layer

This folder maps executor keys to OCaml executor implementations.

Responsibilities:
- define executor contracts
- register executor implementations
- hold per-node-type behavior such as `add`

Non-goals:
- graph storage and validation
- JSON spec decoding
- worker transport
