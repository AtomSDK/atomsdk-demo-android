package com.atom.vpn.demo

/**
 * A generic callback interface for handling asynchronous results in the Atom Demo App.
 *
 * @param T The type of result expected when the callback is invoked.
 */
interface AtomDemoAppCallback<T> {

    /**
     * Invokes the callback with a result.
     *
     * @param result The result of type [T] provided to the callback.
     */
    fun invoke(result: T?)
}
