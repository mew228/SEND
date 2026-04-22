let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_number 0 context.Executor.inputs in
  Executor.single_output 0 (Node.normalize_number (-.value))

let executor = { Executor.key = "negate"; run }
