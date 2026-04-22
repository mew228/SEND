module String_map = Stdlib.Map.Make (String)

type t = {
  node_specs : Node.node_spec list;
  nodes : Node.t list;
  edges : Node.edge list;
  mutable port_values : Node.value Node.Port_ref_map.t;
}

let create ?(port_values = []) ~node_specs ~nodes ~edges () =
  let initial_port_values =
    List.fold_left
      (fun current (port_ref, value) -> Node.Port_ref_map.add port_ref value current)
      Node.Port_ref_map.empty
      port_values
  in
  { node_specs; nodes; edges; port_values = initial_port_values }

let node_specs graph = graph.node_specs
let nodes graph = graph.nodes
let edges graph = graph.edges

let build_node_map nodes =
  List.fold_left
    (fun current (node : Node.t) -> Node_id.Map.add node.id node current)
    Node_id.Map.empty
    nodes

let build_spec_map specs =
  List.fold_left
    (fun current (spec : Node.node_spec) -> String_map.add spec.node_type spec current)
    String_map.empty
    specs

let find_node graph node_id =
  Node_id.Map.find_opt node_id (build_node_map graph.nodes)

let find_node_spec graph node_type =
  String_map.find_opt node_type (build_spec_map graph.node_specs)

let node_spec_for_node node_specs_by_type (node : Node.t) =
  String_map.find_opt node.node_type node_specs_by_type

let expected_port_kind graph port_ref =
  match find_node graph port_ref.Node.node_id with
  | None -> None
  | Some node -> (
      match find_node_spec graph node.node_type with
      | None -> None
      | Some spec ->
          match Node.find_output_port_spec spec port_ref.port_index with
          | Some port -> Some port.value_kind
          | None -> (
              match Node.find_input_port_spec spec port_ref.port_index with
              | Some port -> Some port.value_kind
              | None -> None))

let port_value graph port_ref =
  Node.Port_ref_map.find_opt port_ref graph.port_values

let set_port_value graph port_ref value =
  match expected_port_kind graph port_ref with
  | Some expected_kind when Node.kind_matches_value expected_kind value ->
      graph.port_values <- Node.Port_ref_map.add port_ref value graph.port_values;
      Ok ()
  | Some expected_kind ->
      Error
        [
          Graph_error.Invalid_port_value
            {
              port = port_ref;
              expected_kind;
              actual_value = value;
            };
        ]
  | None ->
      Error [ Graph_error.Invalid_target_port port_ref ]

let clear_port_values graph =
  graph.port_values <- Node.Port_ref_map.empty

let validate_duplicate_nodes nodes =
  let rec loop seen errors = function
    | [] -> List.rev errors
    | (node : Node.t) :: remaining ->
        let errors =
          if Node_id.Set.mem node.id seen then
            Graph_error.Duplicate_node_id node.id :: errors
          else
            errors
        in
        let seen = Node_id.Set.add node.id seen in
        loop seen errors remaining
  in
  loop Node_id.Set.empty [] nodes

let validate_duplicate_specs specs =
  let rec loop seen errors = function
    | [] -> List.rev errors
    | (spec : Node.node_spec) :: remaining ->
        let errors =
          if String_map.mem spec.node_type seen then
            Graph_error.Duplicate_node_type spec.node_type :: errors
          else
            errors
        in
        let seen = String_map.add spec.node_type spec seen in
        loop seen errors remaining
  in
  loop String_map.empty [] specs

let validate_node_specs nodes node_specs_by_type =
  List.filter_map
    (fun (node : Node.t) ->
      if String_map.mem node.node_type node_specs_by_type then
        None
      else
        Some (Graph_error.Missing_node_spec node.node_type))
    nodes

let validate_node_data_fields node_specs_by_type nodes =
  List.concat_map
    (fun (node : Node.t) ->
      match node_spec_for_node node_specs_by_type node with
      | None -> []
      | Some spec ->
          List.filter_map
            (fun (field : Node.data_field) ->
              match Node.find_data_field_spec spec field.name with
              | None ->
                  Some
                    (Graph_error.Unknown_data_field
                       {
                         node_id = node.id;
                         field_name = field.name;
                       })
              | Some field_spec when Node.kind_matches_value field_spec.value_kind field.value -> None
              | Some field_spec ->
                  Some
                    (Graph_error.Invalid_node_data_field
                       {
                         node_id = node.id;
                         field_name = field.name;
                         expected_kind = field_spec.value_kind;
                         actual_value = field.value;
                       }))
            node.data_fields)
    nodes

let incoming_edges_for_port edges node_id port_index =
  List.filter
    (fun (edge : Node.edge) ->
      Node_id.compare edge.target.node_id node_id = 0 && edge.target.port_index = port_index)
    edges

let validate_input_port_arities graph node_specs_by_type nodes =
  List.concat_map
    (fun (node : Node.t) ->
      match node_spec_for_node node_specs_by_type node with
      | None -> []
      | Some spec ->
          spec.Node.input_ports
          |> List.filter_map (fun (port : Node.port_spec) ->
                 let incoming_edges = incoming_edges_for_port graph.edges node.id port.index in
                 let incoming_count = List.length incoming_edges in
                 match port.arity with
                 | Node.One ->
                     if incoming_count <= 1 then
                       None
                     else
                       Some
                         (Graph_error.Too_many_incoming_edges
                            {
                              node_id = node.id;
                              port_index = port.index;
                              actual_count = incoming_count;
                            })
                 | Node.Many ->
                     if List.length spec.Node.input_ports <> 1 then
                       Some
                         (Graph_error.Invalid_multi_input_arity
                            {
                              node_id = node.id;
                              port_index = port.index;
                            })
                     else if incoming_count = 0 then
                       Some
                         (Graph_error.Missing_multi_input_connection
                            {
                              node_id = node.id;
                              port_index = port.index;
                            })
                     else
                       None))
    nodes

let validate_edge nodes_by_id node_specs_by_type (edge : Node.edge) =
  match Node_id.Map.find_opt edge.source.node_id nodes_by_id with
  | None -> [ Graph_error.Missing_source_node edge.source.node_id ]
  | Some source_node -> (
      match Node_id.Map.find_opt edge.target.node_id nodes_by_id with
      | None -> [ Graph_error.Missing_target_node edge.target.node_id ]
      | Some target_node -> (
          match
            ( node_spec_for_node node_specs_by_type source_node,
              node_spec_for_node node_specs_by_type target_node )
          with
          | None, _ -> [ Graph_error.Missing_node_spec source_node.node_type ]
          | _, None -> [ Graph_error.Missing_node_spec target_node.node_type ]
          | Some source_spec, Some target_spec -> (
              match
                ( Node.find_output_port_spec source_spec edge.source.port_index,
                  Node.find_input_port_spec target_spec edge.target.port_index )
              with
              | None, _ -> [ Graph_error.Invalid_source_port edge.source ]
              | _, None -> [ Graph_error.Invalid_target_port edge.target ]
              | Some source_port, Some target_port ->
                  if Node.kinds_compatible source_port.value_kind target_port.value_kind then
                    []
                  else
                    [
                      Graph_error.Incompatible_edge_types
                        {
                          source = edge.source;
                          target = edge.target;
                          source_kind = source_port.value_kind;
                          target_kind = target_port.value_kind;
                        };
                    ])))

let validate_port_values graph =
  graph.port_values
  |> Node.Port_ref_map.bindings
  |> List.filter_map (fun (port_ref, value) ->
         match expected_port_kind graph port_ref with
         | Some expected_kind when Node.kind_matches_value expected_kind value -> None
         | Some expected_kind ->
             Some
               (Graph_error.Invalid_port_value
                  {
                    port = port_ref;
                    expected_kind;
                    actual_value = value;
                  })
         | None ->
             Some (Graph_error.Invalid_target_port port_ref))

let validate graph =
  let nodes_by_id = build_node_map graph.nodes in
  let node_specs_by_type = build_spec_map graph.node_specs in
  let errors =
    validate_duplicate_nodes graph.nodes
    @ validate_duplicate_specs graph.node_specs
    @ validate_node_specs graph.nodes node_specs_by_type
    @ validate_node_data_fields node_specs_by_type graph.nodes
    @ List.concat_map (validate_edge nodes_by_id node_specs_by_type) graph.edges
    @ validate_input_port_arities graph node_specs_by_type graph.nodes
    @ validate_port_values graph
  in
  match errors with
  | [] -> Ok ()
  | errors -> Error errors

let downstream graph port_ref =
  graph.edges
  |> List.filter_map (fun (edge : Node.edge) ->
         if Node.compare_port_ref edge.source port_ref = 0 then Some edge.target else None)

let incoming_count graph node_id =
  graph.edges
  |> List.fold_left
       (fun count (edge : Node.edge) ->
         if Node_id.compare edge.target.node_id node_id = 0 then count + 1 else count)
       0

let topological_sort graph =
  let nodes_by_id = build_node_map graph.nodes in
  let initial_indegree =
    graph.nodes
    |> List.fold_left (fun current (node : Node.t) -> Node_id.Map.add node.id 0 current) Node_id.Map.empty
    |> fun indegree ->
    List.fold_left
      (fun current (edge : Node.edge) ->
        let existing =
          match Node_id.Map.find_opt edge.target.node_id current with
          | Some count -> count
          | None -> 0
        in
        Node_id.Map.add edge.target.node_id (existing + 1) current)
      indegree
      graph.edges
  in
  let initial_queue =
    initial_indegree
    |> Node_id.Map.bindings
    |> List.filter_map (fun (node_id, count) -> if count = 0 then Some node_id else None)
  in
  let outgoing_edges_for_node node_id =
    List.filter
      (fun (edge : Node.edge) -> Node_id.compare edge.source.node_id node_id = 0)
      graph.edges
  in
  let rec loop queue indegree ordered =
    match queue with
    | [] ->
        if List.length ordered = List.length graph.nodes then
          ordered
          |> List.rev
          |> List.filter_map (fun node_id -> Node_id.Map.find_opt node_id nodes_by_id)
          |> fun nodes -> Ok nodes
        else
          indegree
          |> Node_id.Map.bindings
          |> List.filter_map (fun (node_id, count) -> if count > 0 then Some node_id else None)
          |> fun cyclic_nodes -> Error [ Graph_error.Cycle_detected cyclic_nodes ]
    | node_id :: remaining ->
        let indegree, released =
          outgoing_edges_for_node node_id
          |> List.fold_left
               (fun (current_indegree, current_released) (edge : Node.edge) ->
                 let target_id = edge.target.node_id in
                 let existing =
                   match Node_id.Map.find_opt target_id current_indegree with
                   | Some count -> count
                   | None -> 0
                 in
                 let updated = existing - 1 in
                 let current_indegree = Node_id.Map.add target_id updated current_indegree in
                 if updated = 0 then
                   (current_indegree, target_id :: current_released)
                 else
                   (current_indegree, current_released))
               (indegree, [])
        in
        loop (remaining @ List.rev released) indegree (node_id :: ordered)
  in
  loop initial_queue initial_indegree []
