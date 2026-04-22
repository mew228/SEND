let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* numerator = Executor.expect_number 0 context.Executor.inputs in
  let* denominator = Executor.expect_number 1 context.inputs in
  if denominator = 0.0 then
    Error (Executor.Message "Division by zero")
  else
    Executor.single_output 0 (Node.normalize_number (numerator /. denominator))

let executor = { Executor.key = "divide"; run }
