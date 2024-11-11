package org.shypl.tool.app.holders

import java.util.concurrent.locks.ReadWriteLock
import kotlin.concurrent.withLock

@Suppress("DuplicatedCode")
open class SafePositiveIntHolder<R>(
	value: Int,
	private val lock: ReadWriteLock,
) {
	@Volatile
	private var current = value
	
	val value get() = lock.readLock().withLock { current }
	
	protected open fun getMaximumValue() = Int.MAX_VALUE
	
	protected open fun onChange(old: Int, new: Int, reason: R) {}
	
	protected open fun afterChange(old: Int, new: Int, reason: R) {}
	
	fun set(value: Int, reason: R): Boolean {
		require(value >= 0) { "Negative value" }
		
		val changed: Boolean
		val old: Int
		var new = value
		
		lock.writeLock().withLock {
			val max = getMaximumValue()
			if (new > max) {
				new = max
			}
			
			old = current
			if (new == old) {
				changed = false
			}
			else {
				changed = true
				current = new
				onChange(old, new, reason)
			}
		}
		
		if (changed) {
			afterChange(old, new, reason)
		}
		
		return changed
	}
	
	fun add(value: Int, reason: R): Int {
		if (value == 0) return current
		
		require(value > 0) { "Negative value" }
		
		var changed: Boolean
		val old: Int
		var new: Int
		
		lock.writeLock().withLock {
			old = current
			val max = getMaximumValue()
			if (old == max) {
				changed = false
				new = old
			}
			else {
				new = old + value
				if (new > max || new < old) {
					new = max
				}
				changed = true
				current = new
				onChange(old, new, reason)
			}
		}
		
		if (changed) {
			afterChange(old, new, reason)
		}
		
		return new
	}
	
	fun take(value: Int, reason: R): Boolean {
		if (value == 0) return true
		
		require(value > 0) { "Negative value" }
		
		val changed: Boolean
		val old: Int
		val new: Int
		
		lock.writeLock().withLock {
			old = current
			if (old >= value) {
				changed = true
				new = old - value
				current = new
				onChange(old, new, reason)
			}
			else {
				changed = false
				new = old
			}
		}
		
		if (changed) {
			afterChange(old, new, reason)
		}
		
		return changed
	}
}

fun SafePositiveIntHolder<Unit>.set(value: Int) = set(value, Unit)
fun SafePositiveIntHolder<Unit>.add(value: Int) = add(value, Unit)
fun SafePositiveIntHolder<Unit>.take(value: Int) = take(value, Unit)
