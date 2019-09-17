# trace-encoder

This module contains a small library for generating trace files that can be viewed with Chrome by
visiting `chrome://tracing`. Trace events are represented by the various subclasses of `TraceEvent`,
and are encoded by a `TraceEncoder`. The trace file format is described in [this doc][1].

The trace format is designed for low-level profiling data, so events are grouped by process and
thread. `TraceEncoder` lets you create `TraceLogger`s given process and thread names. All events
logged to a `TraceLogger` are grouped under those process and thread names.

[1]: https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU
