let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_value 0 context.Executor.inputs in
  Executor.single_output 0 (Node.String_value (Node.value_to_string value))

let executor = { Executor.key = "to_string"; run }
