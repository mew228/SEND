let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* value = Executor.expect_value 0 context.Executor.inputs in
  match value with
  | Node.Number_value number -> Executor.single_output 0 (Node.normalize_number number)
  | Node.Bool_value boolean ->
      Executor.single_output 0 (Node.normalize_number (if boolean then 1.0 else 0.0))
  | Node.String_value string -> (
      match float_of_string_opt (String.trim string) with
      | Some number -> Executor.single_output 0 (Node.normalize_number number)
      | None -> Error (Executor.Message ("Cannot convert string to number: " ^ string)))

let executor = { Executor.key = "to_number"; run }
