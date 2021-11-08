package com.p2m.core.internal.graph

import com.p2m.core.internal.execution.BeginDirection
import com.p2m.core.internal.execution.Executor
import java.util.concurrent.*

internal abstract class AbsGraphExecutor<KEY, NODE : Node<NODE>, GRAPH : Graph<KEY, NODE>> : Executor {

    private var ownerThread: Thread? = null
    private var quit = false
    abstract val graph: GRAPH
    abstract val messageQueue: BlockingQueue<Runnable>

    override fun runningAndLoop(direction: BeginDirection) {
        ownerThread = Thread.currentThread()
        quit = false

        runGraph(graph, direction) {
            onCompletedForGraph(graph)
        }

        while (true) {
            val runnable = messageQueue.take()
            runnable.run()
            if (runnable is ExitRunnable) {
                break
            }
        }

        quit = true
        ownerThread = null
    }

    override fun quitLoop(runnable: Runnable) {
        postTask(object : ExitRunnable {
            override fun run() {
                runnable.run()
            }
        })
    }

    override fun postTask(runnable: Runnable) {
        check(!quit) { "Not post task, exit already." }

        if (ownerThread === Thread.currentThread() && runnable !is ExitRunnable) {
            runnable.run()
            return
        }
        messageQueue.put(runnable)
    }

    private fun runGraph(graph: GRAPH, direction: BeginDirection, onComplete: () -> Unit) {
        val function = { stage: Stage<NODE> ->
            check(!stage.hasRing) {
                "Cannot be interdependent：" + stage.ringNodes!!
                    .map { "[${it.key.name}, ${it.value.name}]" }
                    .joinToString()
            }
            runStage(stage) {
                val count = graph.stageCompletedCount.incrementAndGet()
                onCompletedForStage(stage)
                if (graph.stageSize == count) {
                    quitLoop(Runnable { onComplete() })
                }
            }
        }
        when (direction) {
            BeginDirection.HEAD -> graph.eachStageBeginFromHead(function)
            BeginDirection.TAIL -> graph.eachStageBeginFromTail(function)
        }
    }

    private fun runStage(stage: Stage<NODE>, onComplete: () -> Unit) {
        if (stage.isEmpty) return
        check(!stage.hasRing) { "Prohibit interdependence between nodes." }
        stage.nodes?.run {
            forEach { node ->
                runNode(node) {
                    val count = stage.completedCount.incrementAndGet()
                    onCompletedForNode(node)
                    if ((stage.nodes?.size ?: 0) == count) {
                        onComplete()
                    }
                }
            }
        }
    }

    abstract fun runNode(node: NODE, onDependsNodeComplete: () -> Unit)

    abstract fun onCompletedForGraph(graph: GRAPH)

    abstract fun onCompletedForStage(stage: Stage<NODE>)

    abstract fun onCompletedForNode(node: NODE)

    private interface ExitRunnable : Runnable
}


