let assert_true condition message =
  if not condition then failwith message

let expect_ok = function
  | Ok value -> value
  | Error errors ->
      failwith (String.concat " | " (List.map Graph_error.to_string errors))

let node_id value = Node_id.of_string value
let port ?(arity = Node.one) index name value_kind =
  Node.make_port_spec ~index ~name ~arity ~value_kind

let data_field_spec ?(required = false) ?default_value name value_kind =
  Node.make_data_field_spec ~name ~value_kind ~required ~default_value

let data_field name value = Node.make_data_field ~name ~value

let spec node_type executor_key ~inputs ~outputs ~data_fields ~display_name =
  Node.make_spec
    ~node_type
    ~set:"primitive"
    ~display_name
    ~description:display_name
    ~input_ports:inputs
    ~output_ports:outputs
    ~data_fields
    ~executor_key

let node ?(data_fields = []) id node_type =
  Node.make ~id:(node_id id) ~node_type ~data_fields ()

let port_ref node_id_value port_index =
  Node.make_port_ref ~node_id:(node_id node_id_value) ~port_index

let edge ~source_node ~source_port ~target_node ~target_port =
  Node.make_edge
    ~source:(port_ref source_node source_port)
    ~target:(port_ref target_node target_port)

let base_specs =
  [
    spec
      "const_number"
      "const_number"
      ~display_name:"Const Number"
      ~inputs:[]
      ~outputs:[ port 0 "value" Node.number_kind ]
      ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Number_value 0.0) "value" Node.number_kind ];
    spec
      "const_bool"
      "const_bool"
      ~display_name:"Const Bool"
      ~inputs:[]
      ~outputs:[ port 0 "value" Node.bool_kind ]
      ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Bool_value false) "value" Node.bool_kind ];
    spec
      "to_string"
      "to_string"
      ~display_name:"To String"
      ~inputs:[ port 0 "value" Node.any_kind ]
      ~outputs:[ port 0 "text" Node.string_kind ]
      ~data_fields:[];
    spec
      "add"
      "add"
      ~display_name:"Add"
      ~inputs:[ port 0 "a" Node.number_kind; port 1 "b" Node.number_kind ]
      ~outputs:[ port 0 "sum" Node.number_kind ]
      ~data_fields:[];
    spec
      "average"
      "average"
      ~display_name:"Average"
      ~inputs:[ port ~arity:Node.many 0 "values" Node.number_kind ]
      ~outputs:[ port 0 "average" Node.number_kind ]
      ~data_fields:[];
  ]

let test_port_specs_preserved () =
  let add_spec =
    match List.find_opt (fun (spec : Node.node_spec) -> spec.node_type = "add") base_specs with
    | Some spec -> spec
    | None -> failwith "missing add spec"
  in
  assert_true (List.length add_spec.input_ports = 2) "expected two input ports";
  assert_true (List.length add_spec.output_ports = 1) "expected one output port";
  assert_true
    (Node.find_input_port_spec add_spec 1 = Some (port 1 "b" Node.number_kind))
    "expected input port lookup";
  assert_true
    (Node.find_output_port_spec add_spec 0 = Some (port 0 "sum" Node.number_kind))
    "expected output port lookup";
  assert_true
    (Node.find_data_field_spec (List.hd base_specs) "value"
    = Some (data_field_spec ~required:true ~default_value:(Node.Number_value 0.0) "value" Node.number_kind))
    "expected data field lookup";
  assert_true (Node.find_input_port_spec add_spec 9 = None) "expected missing port lookup"

let test_validate_unique_nodes_and_types () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:
        [
          node ~data_fields:[ data_field "value" (Node.Number_value 1.0) ] "a" "const_number";
          node "b" "to_string";
        ]
      ~edges:[ edge ~source_node:"a" ~source_port:0 ~target_node:"b" ~target_port:0 ]
      ()
  in
  expect_ok (Graph.validate graph)

let test_any_input_accepts_bool () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:
        [
          node ~data_fields:[ data_field "value" (Node.Bool_value true) ] "a" "const_bool";
          node "b" "to_string";
        ]
      ~edges:[ edge ~source_node:"a" ~source_port:0 ~target_node:"b" ~target_port:0 ]
      ()
  in
  expect_ok (Graph.validate graph)

let test_duplicate_node_ids_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "a" "const_bool" ]
      ~edges:[]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Duplicate_node_id duplicate ] ->
      assert_true (Node_id.to_string duplicate = "a") "wrong duplicate node id"
  | _ -> failwith "expected duplicate node id error"

let test_missing_node_spec_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "missing_spec" ]
      ~edges:[]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Missing_node_spec node_type ] ->
      assert_true (node_type = "missing_spec") "wrong missing node spec"
  | _ -> failwith "expected missing node spec error"

let test_invalid_source_port_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "b" "add" ]
      ~edges:[ edge ~source_node:"a" ~source_port:9 ~target_node:"b" ~target_port:0 ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Invalid_source_port bad_port_ref ] ->
      assert_true (bad_port_ref.port_index = 9) "wrong invalid source port"
  | _ -> failwith "expected invalid source port error"

let test_invalid_target_port_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "b" "add" ]
      ~edges:[ edge ~source_node:"a" ~source_port:0 ~target_node:"b" ~target_port:9 ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Invalid_target_port bad_port_ref ] ->
      assert_true (bad_port_ref.port_index = 9) "wrong invalid target port"
  | _ -> failwith "expected invalid target port error"

let test_type_mismatch_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_bool"; node "b" "add" ]
      ~edges:[ edge ~source_node:"a" ~source_port:0 ~target_node:"b" ~target_port:0 ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Incompatible_edge_types { source_kind = Node.Bool_kind; target_kind = Node.Number_kind; _ } ] -> ()
  | _ -> failwith "expected incompatible edge types error"

let test_node_data_field_validation () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node ~data_fields:[ data_field "value" (Node.String_value "bad") ] "a" "const_number" ]
      ~edges:[]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Invalid_node_data_field { field_name = "value"; expected_kind = Node.Number_kind; actual_value = Node.String_value _; _ } ] -> ()
  | _ -> failwith "expected invalid node data field error"

let test_state_type_validation () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number" ]
      ~edges:[]
      ()
  in
  let output_port = port_ref "a" 0 in
  expect_ok (Graph.set_port_value graph output_port (Node.Number_value 42.0));
  assert_true
    (Graph.port_value graph output_port = Some (Node.Number_value 42.0))
    "expected stored numeric value";
  match Graph.set_port_value graph output_port (Node.Bool_value true) with
  | Error [ Graph_error.Invalid_port_value { expected_kind = Node.Number_kind; actual_value = Node.Bool_value _; _ } ] -> ()
  | _ -> failwith "expected invalid port value error"

let test_initial_state_validation () =
  let invalid_port = port_ref "a" 0 in
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number" ]
      ~edges:[]
      ~port_values:[ (invalid_port, Node.String_value "bad") ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Invalid_port_value { expected_kind = Node.Number_kind; actual_value = Node.String_value _; _ } ] -> ()
  | _ -> failwith "expected invalid initial port value error"

let test_topological_sort () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "b" "const_number"; node "c" "add" ]
      ~edges:
        [
          edge ~source_node:"a" ~source_port:0 ~target_node:"c" ~target_port:0;
          edge ~source_node:"b" ~source_port:0 ~target_node:"c" ~target_port:1;
        ]
      ()
  in
  let ordered = Graph.topological_sort graph |> expect_ok in
  let ordered_ids = List.map (fun (instance : Node.t) -> Node_id.to_string instance.id) ordered in
  assert_true (ordered_ids = [ "a"; "b"; "c" ]) "unexpected topological ordering"

let test_cycle_detection () =
  let cycle_specs =
    [
      spec
        "forward_number"
        "forward_number"
        ~display_name:"Forward"
        ~inputs:[ port 0 "in" Node.number_kind ]
        ~outputs:[ port 0 "out" Node.number_kind ]
        ~data_fields:[];
    ]
  in
  let graph =
    Graph.create
      ~node_specs:cycle_specs
      ~nodes:[ node "a" "forward_number"; node "b" "forward_number" ]
      ~edges:
        [
          edge ~source_node:"a" ~source_port:0 ~target_node:"b" ~target_port:0;
          edge ~source_node:"b" ~source_port:0 ~target_node:"a" ~target_port:0;
        ]
      ()
  in
  match Graph.topological_sort graph with
  | Error [ Graph_error.Cycle_detected node_ids ] ->
      let ids = List.map Node_id.to_string node_ids in
      assert_true (ids = [ "a"; "b" ]) "unexpected cycle members"
  | _ -> failwith "expected cycle detection"

let test_single_input_port_rejects_multiple_edges () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "b" "const_number"; node "c" "add" ]
      ~edges:
        [
          edge ~source_node:"a" ~source_port:0 ~target_node:"c" ~target_port:0;
          edge ~source_node:"b" ~source_port:0 ~target_node:"c" ~target_port:0;
        ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Too_many_incoming_edges { node_id; port_index = 0; actual_count = 2 } ] ->
      assert_true (Node_id.to_string node_id = "c") "wrong node for incoming edge count"
  | _ -> failwith "expected too many incoming edges error"

let test_many_input_port_accepts_multiple_edges () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_number"; node "b" "const_number"; node "avg" "average" ]
      ~edges:
        [
          edge ~source_node:"a" ~source_port:0 ~target_node:"avg" ~target_port:0;
          edge ~source_node:"b" ~source_port:0 ~target_node:"avg" ~target_port:0;
        ]
      ()
  in
  expect_ok (Graph.validate graph)

let test_many_input_port_must_be_only_input () =
  let invalid_spec =
    spec
      "invalid_many"
      "invalid_many"
      ~display_name:"Invalid Many"
      ~inputs:[ port ~arity:Node.many 0 "values" Node.number_kind; port 1 "other" Node.number_kind ]
      ~outputs:[ port 0 "result" Node.number_kind ]
      ~data_fields:[]
  in
  let graph =
    Graph.create
      ~node_specs:(invalid_spec :: base_specs)
      ~nodes:[ node "bad" "invalid_many" ]
      ~edges:[]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Invalid_multi_input_arity { node_id; port_index = 0 } ] ->
      assert_true (Node_id.to_string node_id = "bad") "wrong node for invalid MANY port"
  | _ -> failwith "expected invalid MANY input arity error"

let test_many_input_port_requires_connection () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "avg" "average" ]
      ~edges:[]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Missing_multi_input_connection { node_id; port_index = 0 } ] ->
      assert_true (Node_id.to_string node_id = "avg") "wrong node for missing MANY input"
  | _ -> failwith "expected missing MANY input connection error"

let test_many_input_port_type_mismatch_fail () =
  let graph =
    Graph.create
      ~node_specs:base_specs
      ~nodes:[ node "a" "const_bool"; node "avg" "average" ]
      ~edges:[ edge ~source_node:"a" ~source_port:0 ~target_node:"avg" ~target_port:0 ]
      ()
  in
  match Graph.validate graph with
  | Error [ Graph_error.Incompatible_edge_types { source_kind = Node.Bool_kind; target_kind = Node.Number_kind; _ } ] -> ()
  | _ -> failwith "expected incompatible edge types error for MANY port"

let run_all () =
  test_port_specs_preserved ();
  test_validate_unique_nodes_and_types ();
  test_any_input_accepts_bool ();
  test_duplicate_node_ids_fail ();
  test_missing_node_spec_fail ();
  test_invalid_source_port_fail ();
  test_invalid_target_port_fail ();
  test_type_mismatch_fail ();
  test_node_data_field_validation ();
  test_state_type_validation ();
  test_initial_state_validation ();
  test_topological_sort ();
  test_cycle_detection ();
  test_single_input_port_rejects_multiple_edges ();
  test_many_input_port_accepts_multiple_edges ();
  test_many_input_port_must_be_only_input ();
  test_many_input_port_requires_connection ();
  test_many_input_port_type_mismatch_fail ()
