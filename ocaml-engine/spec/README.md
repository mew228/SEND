# Spec Layer

This folder decodes shared node-spec JSON files into OCaml graph/spec types.

Responsibilities:
- parse JSON node spec files
- normalize external value types into OCaml graph data types
- surface decoding errors separately from graph validation errors
