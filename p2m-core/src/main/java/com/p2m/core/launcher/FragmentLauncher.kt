package com.p2m.core.launcher

import android.app.Fragment
import android.content.Intent
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.internal.launcher.InternalFragmentLauncher
import kotlin.reflect.KProperty

/**
 * A launcher of `Fragment`.
 *
 * For example, has a `Fragment` of showing home in `Main` module:
 * ```kotlin
 * @ApiLauncher("Home")
 * class HomeFragment : Fragment()
 * ```
 *
 * then get a instance for launch in external module:
 * ```kotlin
 * val fragment = P2M.apiOf(Main)
 *      .launcher
 *      .fragmentOfHome
 *      .launch()
 * ```
 *
 * @see ApiLauncher
 */
interface FragmentLauncher<T> {

    class Delegate<T>(createBlock:() -> T) {
        private val real by lazy(LazyThreadSafetyMode.NONE) { InternalFragmentLauncher<T>(createBlock) }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): FragmentLauncher<T> = real
    }

    /**
     * Launch a fragment for that [Fragment] class annotated by [ApiLauncher].
     *
     * @return a instance.
     */
    fun launch(): T
}