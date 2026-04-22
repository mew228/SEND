:::lecture { "id": "logic--getting-started--how-execution-works", "slug": "how-execution-works", "path": { "slug": "logic", "title": "Logic", "description": "Reasoning-focused lessons and graph-building fundamentals." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "How Execution Works", "summary": "Learn how values move through a graph and how nodes run as their inputs become available.", "estimatedMinutes": 12 }

:::sublecture { "id":"execution-begins-when-values-exist", "title":"Execution Begins When Values Exist" }
# Execution Begins When Values Exist

When a graph runs, it does not process the whole graph all at once.

Instead, execution begins wherever values are already available. That is an important idea, because it means a graph runs based on readiness. A node can only do its job when the inputs it depends on already exist.

Some nodes are able to produce values immediately. These are the starting points of execution because they do not need anything upstream before they can run.

Examples include:

- **Constant Boolean**
- **Constant Number**
- **Fetch Price Data**

These nodes act as sources. They either already contain a value or can provide one directly, which means the rest of the graph can begin building from them.

## Why this matters

Execution is really the story of values arriving where they are needed.

A node like **If** cannot choose a branch just because it exists on the canvas. It has to wait until it receives the values it depends on. That means the graph runs step by step, with each new result making later steps possible.

To understand that more clearly, start by building a small branch.

Add a **Fetch Price Data** node and a **Constant Number** node.

Then connect **Constant Boolean** into **If**.

After that, connect **Fetch Price Data** into **If**, and connect **Constant Number** into **If**.

Now the graph contains the pieces for a branch where one condition can eventually choose between a fetched market value and a fallback number.

:::checkpoint
{
  "id": "checkpoint-build-branch-inputs",
  "title": "Checkpoint 1: Build Branch Inputs",
  "instructions": [
    "Add one Fetch Price Data node.",
    "Add one Constant Number node.",
    "Connect Constant Boolean into If.",
    "Connect Fetch Price Data into If.",
    "Connect Constant Number into If."
  ],
  "tasks": [
    {
      "id": "task-add-fetch-for-branch",
      "label": "Add Fetch Price Data",
      "description": "Place a Fetch Price Data node to represent one possible branch value."
    },
    {
      "id": "task-add-fallback-number",
      "label": "Add Constant Number",
      "description": "Place a Constant Number node to represent the fallback branch value."
    },
    {
      "id": "task-connect-bool-into-if",
      "label": "Connect condition into If",
      "description": "Wire Constant Boolean into If."
    },
    {
      "id": "task-connect-price-into-if",
      "label": "Connect price branch into If",
      "description": "Wire Fetch Price Data into If."
    },
    {
      "id": "task-connect-number-into-if",
      "label": "Connect fallback into If",
      "description": "Wire Constant Number into If."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["const_bool", "fetch_price", "const_number", "if"],
    "starterNodes": [
      { "id": "branch-condition", "type": "const_bool", "position": { "x": 90, "y": 120 } },
      { "id": "branch-if", "type": "if", "position": { "x": 520, "y": 220 } }
    ],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "fetch_price" },
    { "type": "node_exists", "nodeType": "const_number" },
    { "type": "connection_exists", "sourceType": "const_bool", "targetType": "if" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "if" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "if" }
  ]
}
:::

:::sublecture { "id":"conditions-decide-how-branches-resolve", "title":"Conditions Decide How Branches Resolve" }
# Conditions Decide How Branches Resolve

Once the graph has possible values ready, it still needs a reason to choose between them.

That is where conditions come in.

Execution in a graph is local and dependency-driven. One node produces a value, another node uses it, and then a later node responds to the result. In a branching setup, the graph must first compute the condition before the **If** node can decide which path to use.

## What happens in order

The flow usually looks like this:

1. source nodes produce values  
2. comparison nodes use those values to compute a signal  
3. flow-control nodes such as **If** use that signal to choose a branch  

That order matters because the branch is not chosen first. The graph has to decide the condition before it can resolve the branch.

## Why the condition is separate

This separation makes the graph easier to read.

You can see the question being asked before you see which path wins. That makes execution much less confusing, especially once graphs become larger.

Now build the condition that will control the branch.

Add a **Greater Than** node.

Connect **Fetch Price Data** into **Greater Than**.

Then connect **Constant Number** into **Greater Than**.

Finally, connect **Greater Than** into **If**.

At that point, the graph has everything it needs to evaluate the condition first and use that result to choose between the branch values.

:::checkpoint
{
  "id": "checkpoint-build-condition-chain",
  "title": "Checkpoint 2: Build The Condition Chain",
  "instructions": [
    "Add a Greater Than node.",
    "Connect Fetch Price Data into Greater Than.",
    "Connect Constant Number into Greater Than.",
    "Connect Greater Than into If."
  ],
  "tasks": [
    {
      "id": "task-add-greater-than-for-condition",
      "label": "Add Greater Than",
      "description": "Place a Greater Than node so the graph can compute the condition before the branch."
    },
    {
      "id": "task-connect-price-into-condition",
      "label": "Connect price into Greater Than",
      "description": "Wire Fetch Price Data into Greater Than."
    },
    {
      "id": "task-connect-threshold-into-condition",
      "label": "Connect threshold into Greater Than",
      "description": "Wire Constant Number into Greater Than."
    },
    {
      "id": "task-connect-condition-into-if",
      "label": "Connect condition into If",
      "description": "Wire Greater Than into If."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "if"],
    "starterNodes": [
      { "id": "execution-price", "type": "fetch_price", "position": { "x": 80, "y": 150 } },
      { "id": "execution-threshold", "type": "const_number", "position": { "x": 80, "y": 310 } },
      { "id": "execution-if", "type": "if", "position": { "x": 600, "y": 230 } }
    ],
    "starterEdges": [
      { "id": "edge-price-to-if", "source": "execution-price", "target": "execution-if" },
      { "id": "edge-threshold-to-if", "source": "execution-threshold", "target": "execution-if" }
    ]
  },
  "validation": [
    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "gt", "targetType": "if" }
  ]
}
:::

:::sublecture { "id":"execution-follows-dependency-order", "title":"Execution Follows Dependency Order" }
# Execution Follows Dependency Order

The most important idea in execution is simple: a node can only run when the values it depends on are ready.

That means execution is not random, and it is not mysterious. It follows dependency order.

A useful way to picture it is as a chain of readiness.

Source nodes are ready first.  
Intermediate nodes become ready when their inputs arrive.  
Downstream nodes become ready after those intermediate results exist.

## A useful mental model

When something feels confusing in a graph, do not ask, “Why is the whole system broken?”

A better question is, “Which value was supposed to arrive here, and did it?”

Most execution problems come from something small in the chain:

- one missing value
- one wrong connection
- one condition that never became ready the way you expected

If you can trace what had to happen first, second, and third, the graph becomes much easier to understand.

## What to remember

Understanding execution is really about understanding dependency order.

The graph runs as values become available. Those values unlock later nodes, and those later nodes create the next results in the chain.

If you can see what each node is waiting on, you can understand how the graph actually runs.
