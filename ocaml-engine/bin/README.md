# Worker Entrypoint

This folder contains the OCaml `stdin`/`stdout` worker entrypoint used by Java.

Protocol notes:
- JSON Lines transport: one request per line on `stdin`
- one response per line on `stdout`
- logs and diagnostics go to `stderr`

Current commands:
- `validate_graph`
- `execute_graph`

Implementation layout:
- `worker.ml`: stdin/stdout loop and logging
- `worker_protocol.ml`: request/response types plus JSON decoding/encoding
- `worker_handler.ml`: command dispatch and per-command handlers
- `tests/`: worker unit and process tests
