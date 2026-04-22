:::lecture { "id": "logic--getting-started--what-is-a-graph-system", "slug": "what-is-a-graph-system", "path": { "slug": "logic", "title": "Logic", "description": "Reasoning-focused lessons and graph-building fundamentals." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "What Is a Graph System?", "summary": "Learn what a graph system is by building your first simple strategy.", "estimatedMinutes": 14 }

:::sublecture { "id":"understanding-the-parts-of-a-graph", "title":"Understanding The Parts Of A Graph" }
# Understanding The Parts Of A Graph

A graph system is a way to build a strategy one step at a time instead of writing everything as one big block.

Rather than hiding the logic, a graph system lays it out visually. Each part of the strategy appears as its own piece, and each connection shows how information moves from one step to the next. That makes the system easier to read, easier to adjust, and much easier to explain.

There are two main parts you need to understand at the beginning: **nodes** and **edges**.

## Nodes

A node is a single step in the graph.

Each node has one job. Some nodes bring in data. Some compare values. Some transform inputs. Some represent actions like buying or selling.

You can think of nodes as the building blocks of the whole strategy. Instead of asking the graph to do everything at once, you place one node for each job you want the graph to perform.

For example:

- **Fetch Price Data** gets market information
- **Constant Number** stores a value you want to compare against
- **Greater Than** checks whether one value is bigger than another
- **Buy** represents an action the graph may trigger

## Edges

Edges are the connections between nodes.

An edge carries the output of one node into the input of another. That is what creates flow. Without edges, the nodes are just separate pieces on a canvas. Once they are connected, the graph begins to behave like a system.

This is the main idea to remember:

**nodes do the work, and edges carry the results forward.**

That means a graph is not just a collection of parts. It is a path of information moving step by step.

## The pattern most graphs follow

Even larger strategies usually follow the same basic pattern:

1. get data  
2. apply logic to that data  
3. send the result toward an action  

That is exactly what you are about to build.

:::checkpoint
{
  "id": "checkpoint-place-core-nodes",
  "title": "Checkpoint 1: Place The Core Parts",
  "instructions": [
    "Add one Fetch Price Data node.",
    "Add one Buy node."
  ],
  "tasks": [
    {
      "id": "task-place-fetch-price",
      "label": "Add Fetch Price Data",
      "description": "Place a Fetch Price Data node on the canvas."
    },
    {
      "id": "task-place-buy-node",
      "label": "Add Buy",
      "description": "Place a Buy node on the canvas."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "buy", "const_number", "gt"],
    "starterNodes": [],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "fetch_price" },
    { "type": "node_exists", "nodeType": "buy" }
  ]
}
:::

:::sublecture { "id":"start-your-first-strategy-with-an-input", "title":"Start Your First Strategy With An Input" }
# Start Your First Strategy With An Input

Before a strategy can decide anything, it needs information.

That is why the first real step in the graph is an input node. In this lesson, that input will be **Fetch Price Data**.

This node brings the current price of a stock into the graph. By itself, it does not make a decision. It simply gives the graph something real to work with.

To make the node useful, you need to configure it. A node placed on the canvas is still general until you tell it what it should look at.

For this example, set the ticker field to `AAPL`.

Now your graph is no longer empty. It has a live market input flowing into it, and that input will become the starting point for the rest of the strategy.

:::checkpoint
{
  "id": "checkpoint-configure-price-input",
  "title": "Checkpoint 2: Configure The Price Input",
  "instructions": [
    "Set the Fetch Price Data node ticker field to AAPL."
  ],
  "tasks": [
    {
      "id": "task-set-price-ticker",
      "label": "Set ticker to AAPL",
      "description": "Update the Fetch Price Data node so its ticker field is AAPL."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "buy"],
    "starterNodes": [
      { "id": "starter-price-input", "type": "fetch_price", "position": { "x": 100, "y": 180 } },
      { "id": "starter-buy-action", "type": "buy", "position": { "x": 560, "y": 210 } }
    ],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_field_equals", "nodeType": "fetch_price", "field": "ticker", "expected": "AAPL" }
  ]
}
:::

:::sublecture { "id":"turn-the-input-into-a-decision", "title":"Turn The Input Into A Decision" }
# Turn The Input Into A Decision

Once the graph has data, the next step is giving that data meaning.

A stock price alone is only a number. A strategy begins when the graph asks a question about that number.

For this first example, the question will be:

**Is the price greater than 150?**

To ask that question, you need two more nodes.

First, add a **Constant Number** node and set its value to `150`. This number acts as the threshold. It gives the graph a line to compare against.

Then add a **Greater Than** node. This is the node that performs the comparison. It takes in two values and checks whether the first one is larger than the second one.

Now connect the pieces together.

Connect **Fetch Price Data** into **Greater Than**.

Then connect **Constant Number** into **Greater Than**.

At this point, the graph is doing more than holding data. It is evaluating data.

If the fetched price is above 150, the output of **Greater Than** becomes true. If it is 150 or lower, the output becomes false.

That true-or-false result is a signal, and signals are what allow the graph to move from raw information into a decision.

:::checkpoint
{
  "id": "checkpoint-build-signal",
  "title": "Checkpoint 3: Build The Signal",
  "instructions": [
    "Add a Constant Number node.",
    "Set the Constant Number node value to 150.",
    "Add a Greater Than node.",
    "Connect Fetch Price Data into Greater Than.",
    "Connect Constant Number into Greater Than."
  ],
  "tasks": [
    {
      "id": "task-add-threshold-node",
      "label": "Add Constant Number",
      "description": "Place a Constant Number node on the canvas."
    },
    {
      "id": "task-set-threshold-value",
      "label": "Set threshold to 150",
      "description": "Update the Constant Number node value to 150."
    },
    {
      "id": "task-add-comparison-node",
      "label": "Add Greater Than",
      "description": "Place a Greater Than node between the input and the action."
    },
    {
      "id": "task-connect-price-to-comparison",
      "label": "Connect price to comparison",
      "description": "Wire Fetch Price Data into Greater Than."
    },
    {
      "id": "task-connect-threshold-to-comparison",
      "label": "Connect threshold to comparison",
      "description": "Wire Constant Number into Greater Than."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "buy"],
    "starterNodes": [
      { "id": "strategy-price", "type": "fetch_price", "position": { "x": 90, "y": 170 } },
      { "id": "strategy-buy", "type": "buy", "position": { "x": 560, "y": 220 } }
    ],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "const_number" },
    { "type": "node_field_equals", "nodeType": "const_number", "field": "value", "expected": 150 },
    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" }
  ]
}
:::

:::sublecture { "id":"complete-the-flow", "title":"Complete The Flow" }
# Complete The Flow

Right now, your graph can answer a question, but it still needs somewhere for that answer to go.

That is where the **Buy** node comes in.

Connect **Greater Than** into **Buy**.

This final connection matters because it completes the chain from information to logic to action. Without it, the graph would know whether the price is above 150, but it would not do anything with that result.

Once that connection is made, the graph becomes a full strategy.

Here is the full flow:

- **Fetch Price Data** gets the price of AAPL
- **Constant Number** stores the value 150
- **Greater Than** checks whether the price is above that value
- **Buy** receives the signal as the action step

In plain language, your graph is now saying:

**If AAPL is greater than 150, trigger a buy decision.**

That is your first complete strategy graph.

:::checkpoint
{
  "id": "checkpoint-complete-first-strategy",
  "title": "Checkpoint 4: Complete The Strategy",
  "instructions": [
    "Connect Greater Than into Buy."
  ],
  "tasks": [
    {
      "id": "task-connect-comparison-to-buy",
      "label": "Connect comparison into Buy",
      "description": "Wire Greater Than into Buy to complete the strategy flow."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "const_number", "gt", "buy"],
    "starterNodes": [
      { "id": "complete-price", "type": "fetch_price", "position": { "x": 90, "y": 170 } },
      { "id": "complete-threshold", "type": "const_number", "position": { "x": 90, "y": 320, "data": { "value": 150 } } },
      { "id": "complete-gt", "type": "gt", "position": { "x": 330, "y": 220 } },
      { "id": "complete-buy", "type": "buy", "position": { "x": 570, "y": 220 } }
    ],
    "starterEdges": [
      { "id": "edge-price-gt", "source": "complete-price", "target": "complete-gt" },
      { "id": "edge-threshold-gt", "source": "complete-threshold", "target": "complete-gt" }
    ]
  },
  "validation": [
    { "type": "connection_exists", "sourceType": "gt", "targetType": "buy" }
  ]
}
:::

:::sublecture { "id":"what-to-remember-about-graph-systems", "title":"What To Remember About Graph Systems" }
# What To Remember About Graph Systems

This lesson introduced the graph system by having you build one yourself.

That is the best way to understand it, because graph systems are not just something you define. They are something you follow.

A graph works by passing information from one step to the next.

One node gathers data.  
Another node provides a rule.  
Another node applies logic.  
Another node represents the action.

Edges carry the results between those steps and turn separate nodes into one flowing system.

That means a graph is not random and it is not just visual decoration. It is a readable structure for reasoning.

As you build larger strategies, keep coming back to these three questions:

- where does the data come from?
- what happens to it?
- what action does it lead to?

If you can answer those clearly, you understand the graph.
