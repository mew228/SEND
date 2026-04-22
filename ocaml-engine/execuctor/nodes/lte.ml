let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* left = Executor.expect_number 0 context.Executor.inputs in
  let* right = Executor.expect_number 1 context.inputs in
  Executor.single_output 0 (Node.Bool_value (left <= right))

let executor = { Executor.key = "lte"; run }
