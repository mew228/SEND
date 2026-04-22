let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_value 0 context.Executor.inputs in
  let result =
    match value with
    | Node.Number_value number -> Node.Bool_value (number <> 0.0)
    | Node.Bool_value boolean -> Node.Bool_value boolean
    | Node.String_value string -> Node.Bool_value (string <> "")
  in
  Executor.single_output 0 result

let executor = { Executor.key = "to_bool"; run }
