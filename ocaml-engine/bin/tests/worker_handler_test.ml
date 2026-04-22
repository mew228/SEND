let assert_true condition message =
  if not condition then failwith message

let primitive_spec node_type display_name description inputs outputs data_fields executor_key =
  `Assoc
    [
      ("nodeType", `String node_type);
      ("set", `String "primitive");
      ("displayName", `String display_name);
      ("description", `String description);
      ("inputs", `List inputs);
      ("outputs", `List outputs);
      ("dataFields", `List data_fields);
      ("executorKey", `String executor_key);
    ]

let port ?(arity = "ONE") index name value_type =
  `Assoc
    [
      ("index", `Int index);
      ("name", `String name);
      ("arity", `String arity);
      ("valueType", `String value_type);
    ]

let data_field name value =
  `Assoc [ ("name", `String name); ("value", value) ]

let execution_payload =
  `Assoc
    [
      ( "graph",
        `Assoc
          [
            ( "nodes",
              `List
                [
                  `Assoc
                    [
                      ("id", `String "a");
                      ("nodeType", `String "const_number");
                      ("dataFields", `List [ data_field "value" (`Int 2) ]);
                    ];
                  `Assoc
                    [
                      ("id", `String "b");
                      ("nodeType", `String "const_number");
                      ("dataFields", `List [ data_field "value" (`Int 3) ]);
                    ];
                  `Assoc
                    [ ("id", `String "c"); ("nodeType", `String "add"); ("dataFields", `List []) ];
                ] );
            ( "edges",
              `List
                [
                  `Assoc
                    [
                      ("id", `String "e-1");
                      ("sourceNode", `String "a");
                      ("sourcePort", `Int 0);
                      ("targetNode", `String "c");
                      ("targetPort", `Int 0);
                    ];
                  `Assoc
                    [
                      ("id", `String "e-2");
                      ("sourceNode", `String "b");
                      ("sourcePort", `Int 0);
                      ("targetNode", `String "c");
                      ("targetPort", `Int 1);
                    ];
                ] );
          ] );
      ( "nodeSpecs",
        `List
          [
            primitive_spec
              "const_number"
              "Constant Number"
              "Outputs a configured numeric constant."
              []
              [ port 0 "value" "NumVal" ]
              [ `Assoc
                  [
                    ("name", `String "value");
                    ("valueType", `String "NumVal");
                    ("required", `Bool true);
                    ("defaultValue", `Int 0);
                  ] ]
              "const_number";
            primitive_spec
              "add"
              "Add"
              "Adds two numbers."
              [ port 0 "a" "NumVal"; port 1 "b" "NumVal" ]
              [ port 0 "sum" "NumVal" ]
              []
              "add";
          ] );
    ]

let average_payload =
  `Assoc
    [
      ( "graph",
        `Assoc
          [
            ( "nodes",
              `List
                [
                  `Assoc
                    [
                      ("id", `String "a");
                      ("nodeType", `String "const_number");
                      ("dataFields", `List [ data_field "value" (`Int 2) ]);
                    ];
                  `Assoc
                    [
                      ("id", `String "b");
                      ("nodeType", `String "const_number");
                      ("dataFields", `List [ data_field "value" (`Int 4) ]);
                    ];
                  `Assoc
                    [
                      ("id", `String "c");
                      ("nodeType", `String "const_number");
                      ("dataFields", `List [ data_field "value" (`Int 6) ]);
                    ];
                  `Assoc
                    [ ("id", `String "avg"); ("nodeType", `String "average"); ("dataFields", `List []) ];
                ] );
            ( "edges",
              `List
                [
                  `Assoc
                    [
                      ("id", `String "e-1");
                      ("sourceNode", `String "a");
                      ("sourcePort", `Int 0);
                      ("targetNode", `String "avg");
                      ("targetPort", `Int 0);
                    ];
                  `Assoc
                    [
                      ("id", `String "e-2");
                      ("sourceNode", `String "b");
                      ("sourcePort", `Int 0);
                      ("targetNode", `String "avg");
                      ("targetPort", `Int 0);
                    ];
                  `Assoc
                    [
                      ("id", `String "e-3");
                      ("sourceNode", `String "c");
                      ("sourcePort", `Int 0);
                      ("targetNode", `String "avg");
                      ("targetPort", `Int 0);
                    ];
                ] );
          ] );
      ( "nodeSpecs",
        `List
          [
            primitive_spec
              "const_number"
              "Constant Number"
              "Outputs a configured numeric constant."
              []
              [ port 0 "value" "NumVal" ]
              [ `Assoc
                  [
                    ("name", `String "value");
                    ("valueType", `String "NumVal");
                    ("required", `Bool true);
                    ("defaultValue", `Int 0);
                  ] ]
              "const_number";
            primitive_spec
              "average"
              "Average"
              "Averages connected numbers."
              [ port ~arity:"MANY" 0 "values" "NumVal" ]
              [ port 0 "average" "NumVal" ]
              []
              "average";
          ] );
    ]

let test_handle_validate_graph () =
  let request =
    {
      Worker_protocol.command = Worker_protocol.Validate_graph;
      payload = Some execution_payload;
    }
  in
  match Worker_handler.handle_request request with
  | Worker_protocol.Success { command = Worker_protocol.Validate_graph; result } ->
      assert_true
        (Yojson.Safe.Util.member "valid" result = `Bool true)
        "expected validate_graph success result"
  | _ -> failwith "expected validate_graph success response"

let test_handle_execute_graph () =
  let request =
    {
      Worker_protocol.command = Worker_protocol.Execute_graph;
      payload = Some execution_payload;
    }
  in
  match Worker_handler.handle_request request with
  | Worker_protocol.Success { command = Worker_protocol.Execute_graph; result } ->
      assert_true
        (Yojson.Safe.Util.member "c" result |> Yojson.Safe.Util.member "sum" = `Int 5)
        "expected add-node execution output"
  | _ -> failwith "expected execute_graph success response"

let test_handle_execute_graph_many_input () =
  let request =
    {
      Worker_protocol.command = Worker_protocol.Execute_graph;
      payload = Some average_payload;
    }
  in
  match Worker_handler.handle_request request with
  | Worker_protocol.Success { command = Worker_protocol.Execute_graph; result } ->
      assert_true
        (Yojson.Safe.Util.member "avg" result |> Yojson.Safe.Util.member "average" = `Int 4)
        "expected average-node execution output"
  | _ -> failwith "expected execute_graph success response for MANY input"

let test_handle_execute_graph_protocol_error () =
  let request =
    {
      Worker_protocol.command = Worker_protocol.Execute_graph;
      payload = Some (`Assoc [ ("graph", `List []) ]);
    }
  in
  match Worker_handler.handle_request request with
  | Worker_protocol.Failure { code; details; _ } ->
      assert_true (code = "protocol_error") "expected protocol_error code";
      assert_true (details = []) "expected no details for malformed protocol payload"
  | _ -> failwith "expected execute_graph failure response"

let test_handle_simulate_graph_protocol_error () =
  let request =
    {
      Worker_protocol.command = Worker_protocol.Simulate_graph;
      payload = Some execution_payload;
    }
  in
  match Worker_handler.handle_request request with
  | Worker_protocol.Failure { code; details; _ } ->
      assert_true (code = "protocol_error") "expected protocol_error code";
      assert_true (details = []) "expected no details for malformed simulation payload"
  | _ -> failwith "expected simulate_graph failure response"

let run_all () =
  test_handle_validate_graph ();
  test_handle_execute_graph ();
  test_handle_execute_graph_many_input ();
  test_handle_execute_graph_protocol_error ();
  test_handle_simulate_graph_protocol_error ()
