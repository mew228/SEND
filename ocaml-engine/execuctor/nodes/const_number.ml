let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_number_field context "value" in
  Executor.single_output 0 (Node.normalize_number value)

let executor = { Executor.key = "const_number"; run }
