let assert_true condition message =
  if not condition then failwith message

let expect_ok = function
  | Ok value -> value
  | Error _ -> failwith "expected successful decode"

let test_decode_validate_graph () =
  let request =
    Worker_protocol.decode_request "{\"command\":\"validate_graph\"}"
    |> expect_ok
  in
  assert_true
    (request.command = Worker_protocol.Validate_graph)
    "expected validate_graph command";
  assert_true (request.payload = None) "expected no payload"

let test_decode_execute_graph () =
  let request =
    Worker_protocol.decode_request
      "{\"command\":\"execute_graph\",\"payload\":{\"entrypoint\":\"main\"}}"
    |> expect_ok
  in
  assert_true
    (request.command = Worker_protocol.Execute_graph)
    "expected execute_graph command";
  assert_true (request.payload <> None) "expected payload to be preserved"

let test_decode_simulate_graph () =
  let request =
    Worker_protocol.decode_request
      "{\"command\":\"simulate_graph\",\"payload\":{\"simulation\":{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\",\"initialCash\":1000,\"includeTrace\":true}}}"
    |> expect_ok
  in
  assert_true
    (request.command = Worker_protocol.Simulate_graph)
    "expected simulate_graph command";
  assert_true (request.payload <> None) "expected payload to be preserved"

let test_invalid_json () =
  match Worker_protocol.decode_request "{" with
  | Error (Worker_protocol.Invalid_json _) -> ()
  | _ -> failwith "expected invalid json error"

let test_missing_command () =
  match Worker_protocol.decode_request "{\"payload\":{}}" with
  | Error Worker_protocol.Missing_command -> ()
  | _ -> failwith "expected missing_command error"

let test_unknown_command () =
  match Worker_protocol.decode_request "{\"command\":\"do_the_thing\"}" with
  | Error (Worker_protocol.Unknown_command "do_the_thing") -> ()
  | _ -> failwith "expected unknown_command error"

let test_invalid_command_type () =
  match Worker_protocol.decode_request "{\"command\":123}" with
  | Error Worker_protocol.Invalid_command_type -> ()
  | _ -> failwith "expected invalid_command_type error"

let run_all () =
  test_decode_validate_graph ();
  test_decode_execute_graph ();
  test_decode_simulate_graph ();
  test_invalid_json ();
  test_missing_command ();
  test_unknown_command ();
  test_invalid_command_type ()
