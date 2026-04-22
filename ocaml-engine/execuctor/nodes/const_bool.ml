let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_bool_field context "value" in
  Executor.single_output 0 (Node.Bool_value value)

let executor = { Executor.key = "const_bool"; run }
