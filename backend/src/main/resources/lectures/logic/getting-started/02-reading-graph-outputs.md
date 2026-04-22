:::lecture { "id": "logic--getting-started--reading-graph-outputs", "slug": "reading-graph-outputs", "path": { "slug": "logic", "title": "Logic", "description": "Reasoning-focused lessons and graph-building fundamentals." }, "category": { "slug": "getting-started", "title": "Getting Started" }, "title": "Reading Graph Outputs", "summary": "Learn how to interpret results and signals from your system.", "estimatedMinutes": 11 }

:::sublecture { "id":"outputs-are-what-nodes-emit", "title":"Outputs Are What Nodes Emit" }
# Outputs Are What Nodes Emit

When you look at a strategy graph, it helps to remember that every node is passing something forward.

A node does not exist just to sit on the canvas. Its job is to take in a value, do something with it, and produce an output that another node can use. That output is how information moves through the graph.

Sometimes the output is a number. Sometimes it is a true-or-false signal. Sometimes it is something that leads directly into an action. The important part is that every connection in the graph is carrying a result from one step to the next.

## Start by asking the right question

When you are trying to read a graph, do not only ask, “What does this node do?”

A better way to think is:

1. what is coming into this node?
2. what is coming out of it?
3. what will the next node do with that result?

That mindset makes graphs much easier to follow, because you stop seeing them as isolated boxes and start seeing them as a chain of meaning.

## A simple example

A good place to start is with the `Abs` node.

The `Abs` node takes in a number and returns its absolute value. In other words, it keeps the size of the number but removes whether it was positive or negative.

That makes it a useful example because nothing dramatic is happening yet. The node is simply transforming one numeric output into another numeric output. This lets you focus on the idea of outputs without also worrying about action logic at the same time.

To practice reading an output, build that small chain first.

Add an **Abs** node.

Then connect **Fetch Price Data** into **Abs**.

Now the graph has a short flow you can read clearly: price data comes in, then the `Abs` node changes that value and passes the result forward.

:::checkpoint
{
  "id": "checkpoint-build-output-transform",
  "title": "Checkpoint 1: Build An Output Transform",
  "instructions": [
    "Add an Abs node.",
    "Connect Fetch Price Data into Abs."
  ],
  "tasks": [
    {
      "id": "task-add-abs-node",
      "label": "Add Abs",
      "description": "Place an Abs node on the canvas."
    },
    {
      "id": "task-connect-price-into-abs",
      "label": "Connect price into Abs",
      "description": "Wire Fetch Price Data into Abs so one output becomes another."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "abs", "const_number", "gt"],
    "starterNodes": [
      { "id": "output-price", "type": "fetch_price", "position": { "x": 100, "y": 220 } }
    ],
    "starterEdges": []
  },
  "validation": [
    { "type": "node_exists", "nodeType": "abs" },
    { "type": "connection_exists", "sourceType": "fetch_price", "targetType": "abs" }
  ]
}
:::

:::sublecture { "id":"signals-are-outputs-too", "title":"Signals Are Outputs Too" }
# Signals Are Outputs Too

Once you understand that numbers can be outputs, the next step is realizing that outputs are not always numeric.

Some outputs are signals.

A signal is usually the result of a comparison. Instead of producing a number like 142 or 300, the node produces an answer like true or false. That may seem simple, but it is one of the most important ideas in a strategy graph, because action nodes often depend on signals like these.

## Turning a value into a signal

This is where graphs start to feel more like reasoning.

You can take a numeric output, compare it against a threshold, and turn it into something meaningful. Instead of just having a value, the graph now has an answer to a question.

For example, after the `Abs` node transforms the value, you might want to ask whether that result is greater than a certain number.

That is what the **Greater Than** node is for.

It compares two values and returns a boolean result. If the first value is larger, the output is true. If it is not, the output is false.

## Build the signal path

Add a **Greater Than** node.

Then connect **Abs** into **Greater Than** so the transformed number becomes one side of the comparison.

Next, connect **Constant Number** into **Greater Than** so the graph has a threshold to compare against.

Now the graph is doing more than transforming data. It is interpreting that data.

You can read the flow like this: the graph receives a price, changes it with `Abs`, compares that result to a fixed number, and produces a signal that says whether the value is above the threshold.

That true-or-false output is something you can actually reason about.

:::checkpoint
{
  "id": "checkpoint-build-signal-from-output",
  "title": "Checkpoint 2: Build A Signal From An Output",
  "instructions": [
    "Add a Greater Than node.",
    "Connect Abs into Greater Than.",
    "Connect Constant Number into Greater Than."
  ],
  "tasks": [
    {
      "id": "task-add-signal-node",
      "label": "Add Greater Than",
      "description": "Place a Greater Than node to turn a numeric output into a signal."
    },
    {
      "id": "task-connect-abs-into-signal",
      "label": "Connect Abs into Greater Than",
      "description": "Wire the transformed output into the comparison."
    },
    {
      "id": "task-connect-threshold-into-signal",
      "label": "Connect threshold into Greater Than",
      "description": "Wire Constant Number into the comparison as the threshold."
    }
  ],
  "sandboxPreset": {
    "allowedNodeTypes": ["fetch_price", "abs", "const_number", "gt"],
    "starterNodes": [
      { "id": "signal-price", "type": "fetch_price", "position": { "x": 80, "y": 170 } },
      { "id": "signal-abs", "type": "abs", "position": { "x": 320, "y": 170 } },
      { "id": "signal-threshold", "type": "const_number", "position": { "x": 320, "y": 320 } }
    ],
    "starterEdges": [
      { "id": "edge-price-abs", "source": "signal-price", "target": "signal-abs" }
    ]
  },
  "validation": [
    { "type": "node_exists", "nodeType": "gt" },
    { "type": "connection_exists", "sourceType": "abs", "targetType": "gt" },
    { "type": "connection_exists", "sourceType": "const_number", "targetType": "gt" }
  ]
}
:::

:::sublecture { "id":"interpret-the-shape-not-just-the-node", "title":"Interpret The Shape, Not Just The Node" }
# Interpret The Shape, Not Just The Node

At this point, the most important lesson is not just what each individual node does. It is how to read the full path from one node to the next.

That is because outputs only make sense in context.

A number means one thing when it is first fetched. It can mean something slightly different after it is transformed. Then it can become a signal after it is compared. The output itself matters, but the bigger meaning comes from where it came from and where it is going next.

## Read the relationship between steps

When you are trying to understand a graph, look at the shape of the chain.

Ask yourself:

- what value entered this step?
- how did this step change it?
- what does the next step assume that output means?

That is how you move from reading single nodes to reading logic.

## Why this helps when something goes wrong

This also makes debugging much easier.

If a graph is behaving strangely, the problem is often not just “this node is bad.” The issue may be somewhere in the flow around it.

Maybe the input value was not what you expected.  
Maybe the threshold was set wrong.  
Maybe the comparison worked correctly, but the wrong signal was connected downstream.

When you understand outputs clearly, you can trace where meaning changed in the graph and find the step that caused the issue.

## The main idea to remember

Outputs are how nodes communicate.

Each node passes a result to the next one, and the next node responds based on what it receives. Once you start reading graphs that way, the whole system becomes easier to understand.

You are no longer just looking at separate components.

You are reading a sequence of information being passed, transformed, interpreted, and used.
