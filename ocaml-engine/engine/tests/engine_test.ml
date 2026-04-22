let assert_true condition message =
  if not condition then failwith message

let expect_ok = function
  | Ok value -> value
  | Error errors ->
      failwith (String.concat " | " (List.map Engine_error.to_string errors))

let node_id value = Node_id.of_string value

let port index name value_kind =
  Node.make_port_spec ~index ~name ~arity:Node.one ~value_kind

let data_field_spec ?(required = false) ?default_value name value_kind =
  Node.make_data_field_spec ~name ~value_kind ~required ~default_value

let spec
    ?(inputs = [])
    ?(outputs = [])
    ?(data_fields = [])
    ~node_type
    ~executor_key
    ~display_name
    ()
  =
  Node.make_spec
    ~node_type
    ~set:"primitive"
    ~display_name
    ~description:display_name
    ~input_ports:inputs
    ~output_ports:outputs
    ~data_fields
    ~executor_key

let const_number_spec =
  spec
    ~node_type:"const_number"
    ~executor_key:"const_number"
    ~display_name:"Const Number"
    ~outputs:[ port 0 "value" Node.number_kind ]
    ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Number_value 0.0) "value" Node.number_kind ]
    ()

let const_bool_spec =
  spec
    ~node_type:"const_bool"
    ~executor_key:"const_bool"
    ~display_name:"Const Bool"
    ~outputs:[ port 0 "value" Node.bool_kind ]
    ~data_fields:[ data_field_spec ~required:true ~default_value:(Node.Bool_value false) "value" Node.bool_kind ]
    ()

let add_spec =
  spec
    ~node_type:"add"
    ~executor_key:"add"
    ~display_name:"Add"
    ~inputs:[ port 0 "a" Node.number_kind; port 1 "b" Node.number_kind ]
    ~outputs:[ port 0 "sum" Node.number_kind ]
    ()

let if_spec =
  spec
    ~node_type:"if"
    ~executor_key:"if"
    ~display_name:"If"
    ~inputs:[ port 0 "condition" Node.bool_kind; port 1 "when_true" Node.any_kind; port 2 "when_false" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.any_kind ]
    ()

let to_number_spec =
  spec
    ~node_type:"to_number"
    ~executor_key:"to_number"
    ~display_name:"To Number"
    ~inputs:[ port 0 "value" Node.any_kind ]
    ~outputs:[ port 0 "result" Node.number_kind ]
    ()

let average_spec =
  spec
    ~node_type:"average"
    ~executor_key:"average"
    ~display_name:"Average"
    ~inputs:[ Node.make_port_spec ~index:0 ~name:"values" ~arity:Node.many ~value_kind:Node.number_kind ]
    ~outputs:[ port 0 "result" Node.number_kind ]
    ()

let graph_specs =
  [ const_number_spec; const_bool_spec; add_spec; if_spec; to_number_spec; average_spec ]

let primitive_registry = Primitive_registry.registry

let test_execute_graph () =
  let graph =
    Graph.create
      ~node_specs:graph_specs
      ~nodes:
        [
          Node.make
            ~id:(node_id "a")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 2.0) ]
            ();
          Node.make
            ~id:(node_id "b")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 3.0) ]
            ();
          Node.make ~id:(node_id "c") ~node_type:"add" ();
        ]
      ~edges:
        [
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "a") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "c") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "b") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "c") ~port_index:1);
        ]
      ()
  in
  let executed_graph = Engine.execute ~simulation:None ~graph ~registry:primitive_registry |> expect_ok in
  let sum_port = Node.make_port_ref ~node_id:(node_id "c") ~port_index:0 in
  assert_true
    (Graph.port_value executed_graph sum_port = Some (Node.Number_value 5.0))
    "expected computed sum"

let test_if_graph () =
  let graph =
    Graph.create
      ~node_specs:graph_specs
      ~nodes:
        [
          Node.make
            ~id:(node_id "cond")
            ~node_type:"const_bool"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Bool_value true) ]
            ();
          Node.make
            ~id:(node_id "left")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 8.0) ]
            ();
          Node.make
            ~id:(node_id "right")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 2.0) ]
            ();
          Node.make ~id:(node_id "pick") ~node_type:"if" ();
        ]
      ~edges:
        [
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "cond") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "pick") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "left") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "pick") ~port_index:1);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "right") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "pick") ~port_index:2);
        ]
      ()
  in
  let executed_graph = Engine.execute ~simulation:None ~graph ~registry:primitive_registry |> expect_ok in
  let result_port = Node.make_port_ref ~node_id:(node_id "pick") ~port_index:0 in
  assert_true
    (Graph.port_value executed_graph result_port = Some (Node.Number_value 8.0))
    "expected true branch value"

let test_const_default_fallback () =
  let graph =
    Graph.create
      ~node_specs:graph_specs
      ~nodes:[ Node.make ~id:(node_id "a") ~node_type:"const_number" () ]
      ~edges:[]
      ()
  in
  let executed_graph = Engine.execute ~simulation:None ~graph ~registry:primitive_registry |> expect_ok in
  let output_port = Node.make_port_ref ~node_id:(node_id "a") ~port_index:0 in
  assert_true
    (Graph.port_value executed_graph output_port = Some (Node.Number_value 0.0))
    "expected constant default value"

let test_conversion_graph () =
  let graph =
    Graph.create
      ~node_specs:graph_specs
      ~nodes:
        [
          Node.make
            ~id:(node_id "source")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 9.0) ]
            ();
          Node.make ~id:(node_id "convert") ~node_type:"to_number" ();
          Node.make
            ~id:(node_id "increment")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 1.0) ]
            ();
          Node.make ~id:(node_id "sum") ~node_type:"add" ();
        ]
      ~edges:
        [
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "source") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "convert") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "convert") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "sum") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "increment") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "sum") ~port_index:1);
        ]
      ()
  in
  let executed_graph = Engine.execute ~simulation:None ~graph ~registry:primitive_registry |> expect_ok in
  let output_port = Node.make_port_ref ~node_id:(node_id "sum") ~port_index:0 in
  assert_true
    (Graph.port_value executed_graph output_port = Some (Node.Number_value 10.0))
    "expected converted number to feed add"

let test_missing_executor () =
  let graph =
    Graph.create
      ~node_specs:[ add_spec ]
      ~nodes:[ Node.make ~id:(node_id "c") ~node_type:"add" () ]
      ~edges:[]
      ()
  in
  let registry = Executor_registry.empty in
  match Engine.execute ~simulation:None ~graph ~registry with
  | Error [ Engine_error.Missing_executor { executor_key = "add"; _ } ] -> ()
  | _ -> failwith "expected missing executor error"

let test_average_graph () =
  let graph =
    Graph.create
      ~node_specs:graph_specs
      ~nodes:
        [
          Node.make
            ~id:(node_id "a")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 2.0) ]
            ();
          Node.make
            ~id:(node_id "b")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 4.0) ]
            ();
          Node.make
            ~id:(node_id "c")
            ~node_type:"const_number"
            ~data_fields:[ Node.make_data_field ~name:"value" ~value:(Node.Number_value 6.0) ]
            ();
          Node.make ~id:(node_id "avg") ~node_type:"average" ();
        ]
      ~edges:
        [
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "a") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "avg") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "b") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "avg") ~port_index:0);
          Node.make_edge
            ~source:(Node.make_port_ref ~node_id:(node_id "c") ~port_index:0)
            ~target:(Node.make_port_ref ~node_id:(node_id "avg") ~port_index:0);
        ]
      ()
  in
  let executed_graph = Engine.execute ~simulation:None ~graph ~registry:primitive_registry |> expect_ok in
  let output_port = Node.make_port_ref ~node_id:(node_id "avg") ~port_index:0 in
  assert_true
    (Graph.port_value executed_graph output_port = Some (Node.Number_value 4.0))
    "expected average node output"

let run_all () =
  test_execute_graph ();
  test_if_graph ();
  test_const_default_fallback ();
  test_conversion_graph ();
  test_missing_executor ();
  test_average_graph ()
