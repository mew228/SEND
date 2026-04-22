let assert_true condition message =
  if not condition then failwith message

let expect_ok = function
  | Ok value -> value
  | Error error -> failwith (Spec_error.to_string error)

let sample_json =
  {|
  {
    "nodeType": "add",
    "set": "primitive",
    "displayName": "Add",
    "description": "Adds two numbers.",
    "inputs": [
      { "index": 0, "name": "a", "arity": "ONE", "valueType": "NumVal" },
      { "index": 1, "name": "b", "arity": "ONE", "valueType": "NumVal" }
    ],
    "outputs": [
      { "index": 0, "name": "sum", "arity": "ONE", "valueType": "NumVal" }
    ],
    "dataFields": [],
    "executorKey": "add"
  }
|}

let value_json =
  {|
  {
    "nodeType": "if",
    "set": "primitive",
    "displayName": "If",
    "description": "If",
    "inputs": [
      { "index": 0, "name": "condition", "arity": "ONE", "valueType": "BoolVal" },
      { "index": 1, "name": "when_true", "arity": "ONE", "valueType": "Value" },
      { "index": 2, "name": "when_false", "arity": "ONE", "valueType": "Value" }
    ],
    "outputs": [
      { "index": 0, "name": "result", "arity": "ONE", "valueType": "Value" }
    ],
    "dataFields": [],
    "executorKey": "if"
  }
|}

let defaulted_data_field_json =
  {|
  {
    "nodeType": "const_number",
    "set": "primitive",
    "displayName": "Constant Number",
    "description": "Constant Number",
    "inputs": [],
    "outputs": [
      { "index": 0, "name": "value", "arity": "ONE", "valueType": "NumVal" }
    ],
    "dataFields": [
      { "name": "value", "valueType": "NumVal", "required": true, "defaultValue": 0 }
    ],
    "executorKey": "const_number"
  }
|}

let test_decode_sample_spec () =
  let spec = Node_spec_decoder.decode_string sample_json |> expect_ok in
  assert_true (spec.Node.node_type = "add") "wrong node type";
  assert_true (spec.executor_key = "add") "wrong executor key";
  assert_true (List.length spec.input_ports = 2) "wrong input port count";
  assert_true
    (Node.find_input_port_spec spec 0
    = Some (Node.make_port_spec ~index:0 ~name:"a" ~arity:Node.one ~value_kind:Node.number_kind))
    "wrong decoded input port"

let test_decode_value_port_spec () =
  let spec = Node_spec_decoder.decode_string value_json |> expect_ok in
  assert_true
    (Node.find_input_port_spec spec 1
    = Some (Node.make_port_spec ~index:1 ~name:"when_true" ~arity:Node.one ~value_kind:Node.any_kind))
    "expected Value to decode as Any";
  assert_true
    (Node.find_output_port_spec spec 0
    = Some (Node.make_port_spec ~index:0 ~name:"result" ~arity:Node.one ~value_kind:Node.any_kind))
    "expected Any output kind"

let test_decode_defaulted_data_field_spec () =
  let spec = Node_spec_decoder.decode_string defaulted_data_field_json |> expect_ok in
  match Node.find_data_field_spec spec "value" with
  | Some field_spec ->
      assert_true (field_spec.value_kind = Node.number_kind) "wrong data field kind";
      assert_true field_spec.required "expected required field";
      assert_true
        (field_spec.default_value = Some (Node.Number_value 0.0))
        "expected numeric default value"
  | None -> failwith "expected decoded data field spec"

let test_unknown_value_type () =
  let json =
    {|{"nodeType":"bad","set":"primitive","displayName":"Bad","description":"Bad","inputs":[],"outputs":[{"index":0,"name":"out","arity":"ONE","valueType":"Mystery"}],"dataFields":[],"executorKey":"bad"}|}
  in
  match Node_spec_decoder.decode_string json with
  | Error (Spec_error.Unknown_value_type "Mystery") -> ()
  | _ -> failwith "expected unknown value type error"

let many_input_json =
  {|
  {
    "nodeType": "average",
    "set": "primitive",
    "displayName": "Average",
    "description": "Averages numbers.",
    "inputs": [
      { "index": 0, "name": "values", "arity": "MANY", "valueType": "NumVal" }
    ],
    "outputs": [
      { "index": 0, "name": "average", "arity": "ONE", "valueType": "NumVal" }
    ],
    "dataFields": [],
    "executorKey": "average"
  }
|}

let test_decode_many_port_spec () =
  let spec = Node_spec_decoder.decode_string many_input_json |> expect_ok in
  assert_true
    (Node.find_input_port_spec spec 0
    = Some (Node.make_port_spec ~index:0 ~name:"values" ~arity:Node.many ~value_kind:Node.number_kind))
    "expected MANY arity to decode"

let test_unknown_port_arity () =
  let json =
    {|{"nodeType":"bad","set":"primitive","displayName":"Bad","description":"Bad","inputs":[{"index":0,"name":"in","arity":"LOTS","valueType":"NumVal"}],"outputs":[],"dataFields":[],"executorKey":"bad"}|}
  in
  match Node_spec_decoder.decode_string json with
  | Error (Spec_error.Unknown_port_arity "LOTS") -> ()
  | _ -> failwith "expected unknown port arity error"

let run_all () =
  test_decode_sample_spec ();
  test_decode_value_port_spec ();
  test_decode_defaulted_data_field_spec ();
  test_unknown_value_type ();
  test_decode_many_port_spec ();
  test_unknown_port_arity ()
