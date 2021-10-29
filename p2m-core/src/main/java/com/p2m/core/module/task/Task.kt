package com.p2m.core.module.task

import android.content.Context
import com.p2m.core.module.SafeModuleApiProvider
import com.p2m.core.module.ModuleInit

/**
 * A task is the smallest unit in a module to perform necessary initialization.
 *
 * It is design for the module complete necessary initialization.
 *
 * Note: Only recommended to execute lightweight work.
 *
 * @param INPUT set [input] when register a task.
 * @param OUTPUT set [output] when completed work, so should set it up in the [onExecute].
 *
 * @see ModuleInit - How to register a task? and how to use a task?
 */
abstract class Task<INPUT, OUTPUT> {

    internal var inputObj: Any? = null

    @Suppress("UNCHECKED_CAST")
    protected val input: INPUT?
        get() = inputObj as? INPUT

    @JvmField
    var output: OUTPUT? = null

    /**
     *
     * The task executing, called after [ModuleInit.onEvaluate] and before [ModuleInit.onExecuted].
     *
     * NOTE: Running in work thread.
     *
     * You can use [taskOutputProvider] get some dependency task output, also can use
     * [moduleApiProvider] get some dependency module.
     *
     * @param taskOutputProvider task output provider
     * @param moduleApiProvider module provider
     *
     * @see TaskOutputProvider TaskOutputProvider - get some task output.
     * @see SafeModuleApiProvider SafeModuleApiProvider - get some module api.
     */
     abstract fun onExecute(context: Context, taskOutputProvider: TaskOutputProvider, moduleApiProvider: SafeModuleApiProvider)
}