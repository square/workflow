# reactor-core

This module contains the core Reactor logic that exposes 
coroutine-based APIs. No POS modules should include this directly
except for `:workflow-rx2`, which contains the adapters for
RxJava2.
