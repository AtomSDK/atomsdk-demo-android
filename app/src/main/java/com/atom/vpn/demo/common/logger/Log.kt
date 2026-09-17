/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 */
package com.atom.vpn.demo.common.logger

/**
 * Helper class for a list (or tree) of LoggerNodes.
 *
 * When this is set as the head of the list, an instance of it can function as a drop-in
 * replacement for [android.util.Log]. Most of the methods in this class serve only to map a
 * method call in Log to its equivalent in [LogNode].
 *
 * The members are annotated [JvmStatic] so that the remaining Java sources can keep calling
 * `Log.e(...)` / `Log.setLogNode(...)` unchanged. Those annotations can be dropped once the
 * migration to Kotlin is complete.
 */
object Log {
    // Grabbing the native values from Android's native logging facilities,
    // to make for easy migration and interop.
    const val NONE = -1
    const val VERBOSE = android.util.Log.VERBOSE
    const val DEBUG = android.util.Log.DEBUG
    const val INFO = android.util.Log.INFO
    const val WARN = android.util.Log.WARN
    const val ERROR = android.util.Log.ERROR
    const val ASSERT = android.util.Log.ASSERT

    /** The beginning of the LogNode topology. */
    @JvmStatic
    var logNode: LogNode? = null

    /**
     * Instructs the LogNode to print the log data provided. Other LogNodes can
     * be chained to the end of the LogNode as desired.
     *
     * @param priority Log level of the data being logged. Verbose, Error, etc.
     * @param tag Tag for for the log data. Can be used to organize log statements.
     * @param msg The actual message to be logged.
     * @param tr If an exception was thrown, this can be sent along for the logging facilities
     *           to extract and print useful information.
     */
    @JvmStatic
    fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        logNode?.println(priority, tag, msg, tr)
    }

    /**
     * Instructs the LogNode to print the log data provided. Other LogNodes can
     * be chained to the end of the LogNode as desired.
     */
    @JvmStatic
    fun println(priority: Int, tag: String?, msg: String?) {
        println(priority, tag, msg, null)
    }

    /** Prints a message at VERBOSE priority. */
    @JvmStatic
    fun v(tag: String?, msg: String?, tr: Throwable?) = println(VERBOSE, tag, msg, tr)

    /** Prints a message at VERBOSE priority. */
    @JvmStatic
    fun v(tag: String?, msg: String?) = v(tag, msg, null)

    /** Prints a message at DEBUG priority. */
    @JvmStatic
    fun d(tag: String?, msg: String?, tr: Throwable?) = println(DEBUG, tag, msg, tr)

    /** Prints a message at DEBUG priority. */
    @JvmStatic
    fun d(tag: String?, msg: String?) = d(tag, msg, null)

    /** Prints a message at INFO priority. */
    @JvmStatic
    fun i(tag: String?, msg: String?, tr: Throwable?) = println(INFO, tag, msg, tr)

    /** Prints a message at INFO priority. */
    @JvmStatic
    fun i(tag: String?, msg: String?) = i(tag, msg, null)

    /** Prints a message at WARN priority. */
    @JvmStatic
    fun w(tag: String?, msg: String?, tr: Throwable?) = println(WARN, tag, msg, tr)

    /** Prints a message at WARN priority. */
    @JvmStatic
    fun w(tag: String?, msg: String?) = w(tag, msg, null)

    /** Prints a message at WARN priority. */
    @JvmStatic
    fun w(tag: String?, tr: Throwable?) = w(tag, null, tr)

    /** Prints a message at ERROR priority. */
    @JvmStatic
    fun e(tag: String?, msg: String?, tr: Throwable?) = println(ERROR, tag, msg, tr)

    /** Prints a message at ERROR priority. */
    @JvmStatic
    fun e(tag: String?, msg: String?) = e(tag, msg, null)

    /** Prints a message at ASSERT priority. */
    @JvmStatic
    fun wtf(tag: String?, msg: String?, tr: Throwable?) = println(ASSERT, tag, msg, tr)

    /** Prints a message at ASSERT priority. */
    @JvmStatic
    fun wtf(tag: String?, msg: String?) = wtf(tag, msg, null)

    /** Prints a message at ASSERT priority. */
    @JvmStatic
    fun wtf(tag: String?, tr: Throwable?) = wtf(tag, null, tr)

    @JvmStatic
    fun clear() {
        logNode?.clear()
    }
}
