package com.p2m.core.internal.module.task

import android.content.Context
import com.p2m.core.internal.graph.Graph
import com.p2m.core.internal.graph.Stage
import com.p2m.core.internal.log.logW
import com.p2m.core.module.SafeModuleApiProvider
import com.p2m.core.module.task.Task
import java.util.concurrent.atomic.AtomicInteger

internal class TaskGraph private constructor(
    val context: Context,
    val moduleName: String,
    private val taskContainer: TaskContainerImpl,
    val SafeModuleApiProvider: SafeModuleApiProvider
) : Graph<TaskNode, Class<out Task<*, *>>> {
    private val tasks: MutableList<Class<out Task<*, *>>> = mutableListOf()
    private val nodes: HashMap<Class<out Task<*, *>>, TaskNode> = HashMap()
    val taskSize
        get() = tasks.size
    override var stageSize = 0
    override var stageCompletedCount = AtomicInteger()

    companion object{
        internal fun create(context: Context, moduleName: String, taskContainer: TaskContainerImpl, SafeModuleApiProvider: SafeModuleApiProvider): TaskGraph {
            return TaskGraph(context, moduleName, taskContainer, SafeModuleApiProvider)
        }
    }

    init {
        collectView()
    }

    private fun genTask(clazz: Class<out Task<*, *>>) {
        tasks.add(clazz)
    }

    private fun Class<out Task<*, *>>.addDepend(dependClazz: Class<out Task<*, *>>) {
        val ownerNode = nodes[this] ?: return
        val node = nodes[dependClazz] ?: return

        if (node.byDependNodes.add(ownerNode)) {
            node.byDependDegree++
        }

        if (ownerNode.dependNodes.add(node)) {
            ownerNode.dependDegree++
        }
    }

    private fun findRingNodes(nodes: Collection<TaskNode>): HashMap<TaskNode, TaskNode> {
        val ringNodes = HashMap<TaskNode, TaskNode>()
        nodes.forEach { node: TaskNode ->
            node.dependNodes.forEach { dependNode: TaskNode ->
                if (dependNode.dependNodes.contains(node)) {
                    if (ringNodes[dependNode] != node) {
                        ringNodes[node] = dependNode
                    }
                }
            }
        }
        return ringNodes
    }

    override fun evaluate():HashMap<Class<out Task<*, *>>, TaskNode>{
        reset()
        layout()
        return nodes
    }
    
    private fun dependTop(){
        val topJavaClass = taskContainer.topTaskClazz
        nodes.filter { it.value.byDependDegree == 0 && it.key !== topJavaClass}
            .keys
            .forEach { topJavaClass.addDepend(it) }
    }

    override fun getHeadStage(): Stage<TaskNode> {
        val stage = Stage<TaskNode>()
        val nodes = evaluate().values
        val noByDependDegreeNodes = ArrayList<TaskNode>()
        nodes.forEach { node ->
            if (node.byDependDegree == 0) {
                noByDependDegreeNodes.add(node)
            }
        }
        stageSize = 1
        stage.nodes = noByDependDegreeNodes
        return stage
    }

    override fun getTailStage(): Stage<TaskNode> {
        val stage = Stage<TaskNode>()
        val nodes = evaluate().values
        val noDependDegreeNodes = ArrayList<TaskNode>()
        nodes.forEach { node ->
            if (node.dependDegree == 0) {
                noDependDegreeNodes.add(node)
            }
        }
        stageSize = 1
        stage.nodes = noDependDegreeNodes
        return stage
    }
    
    override fun eachStageBeginFromTail(block: (stage: Stage<TaskNode>) -> Unit) {
        val nodes = evaluate().values
        while (!nodes.isEmpty()) {
            val stage = Stage<TaskNode>()
            val noDependDegreeNodes = ArrayList<TaskNode>()

            nodes.forEach{ node ->
                if (node.dependDegree == 0) {
                    noDependDegreeNodes.add(node)
                }
            }

            if (noDependDegreeNodes.isEmpty()) {
                stage.hasRing = true
                stage.ringNodes = findRingNodes(nodes)
            }

            stageSize++
            stage.nodes = noDependDegreeNodes
            block(stage)

            noDependDegreeNodes.forEach { node: TaskNode ->
                node.byDependNodes.forEach { byDependNode: TaskNode ->
                    byDependNode.dependDegree--
                }
                nodes.remove(node)
            }
        }
    }
    
    override fun eachStageBeginFromHead(block:(stage:Stage<TaskNode>)->Unit) {
        val nodes = evaluate().values
        while (!nodes.isEmpty()) {
            val stage = Stage<TaskNode>()
            val noByDependDegreeNodes = ArrayList<TaskNode>()

            nodes.forEach{ node ->
                if (node.byDependDegree == 0) {
                    noByDependDegreeNodes.add(node)
                }
            }

            if (noByDependDegreeNodes.isEmpty()) {
                stage.hasRing = true
                stage.ringNodes = findRingNodes(nodes)
            }

            stageSize++
            stage.nodes = noByDependDegreeNodes
            block(stage)

            noByDependDegreeNodes.forEach { node: TaskNode ->
                node.dependNodes.forEach { dependNode: TaskNode ->
                    dependNode.byDependDegree--
                }
                nodes.remove(node)
            }
        }
    }

    private fun reset(){
        resetNode()
        resetStage()
    }

    private fun resetNode(){
        nodes.clear()
    }

    private fun resetStage(){
        stageSize = 0
        stageCompletedCount.set(0)
    }
    
    private fun collectView(){
        genTasks()
    }
    
    private fun layout(){
        genNodes()
        addDepends()
        dependTop()
    }
    
    private fun genTasks() {
        taskContainer.getAll().forEach {
            genTask(it.getOwner())
        }
    }
    
    private fun genNodes() {
        tasks.iterator().forEach {
            val clazz = it
            taskContainer.find(clazz)?.apply {
                val safeTaskProvider = TaskOutputProviderImplForTask(taskContainer, this)
                nodes[clazz] = TaskNode(context, clazz.simpleName, this.ownerInstance, this.input, safeTaskProvider, clazz === taskContainer.topTaskClazz)
            }
        }
    }
    
    private fun addDepends() {
        taskContainer.getAll().map { it.getOwner() to it.getDependencies() }
            .forEach {
                val owner = it.first
                it.second.forEach { dependClazz ->
                    if (!nodes.containsKey(dependClazz)) logW("${owner.canonicalName} depend on ${dependClazz.canonicalName}, but not registered of ${dependClazz.canonicalName}")
                    owner.addDepend(dependClazz)
                }
            }
    }

}