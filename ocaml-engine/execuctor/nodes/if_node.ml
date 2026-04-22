let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* condition = Executor.expect_bool 0 context.Executor.inputs in
  let* when_true = Executor.expect_value 1 context.inputs in
  let* when_false = Executor.expect_value 2 context.inputs in
  if condition then Executor.single_output 0 when_true else Executor.single_output 0 when_false

let executor = { Executor.key = "if"; run }
