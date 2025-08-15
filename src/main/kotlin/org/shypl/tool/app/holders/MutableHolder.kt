package org.shypl.tool.app.holders

interface MutableHolder<V : Number, R> : Holder<V> {
	fun set(value: V, reason: R): Boolean
	
	fun add(value: V, reason: R): V
	
	fun take(value: V, reason: R): Boolean
}

fun <V : Number> MutableHolder<V, Unit>.set(value: V) = set(value, Unit)
fun <V : Number> MutableHolder<V, Unit>.add(value: V) = add(value, Unit)
fun <V : Number> MutableHolder<V, Unit>.take(value: V) = take(value, Unit)