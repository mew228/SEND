let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let run context =
  let* values = Executor.expect_numbers 0 context.Executor.inputs in
  let sum = List.fold_left ( +. ) 0.0 values in
  let count = List.length values |> float_of_int in
  Executor.single_output 0 (Node.normalize_number (sum /. count))

let executor = { Executor.key = "average"; run }
