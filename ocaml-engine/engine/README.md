# Engine Layer

This folder executes validated graphs using the executor registry.

Responsibilities:
- validate graphs before execution
- order nodes for execution
- resolve node specs to executor keys
- invoke executors and write outputs back into graph state
