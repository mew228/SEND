let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_bool 0 context.Executor.inputs in
  Executor.single_output 0 (Node.Bool_value (not value))

let executor = { Executor.key = "not"; run }
