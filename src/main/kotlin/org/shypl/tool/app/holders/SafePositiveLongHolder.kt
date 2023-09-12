package org.shypl.tool.app.holders

@Suppress("DuplicatedCode")
open class SafePositiveLongHolder<R>(
	value: Long,
	private val lock: Any
) {
	@Volatile
	private var current = value
	
	val value get() = current
	
	protected open fun getMaximumValue() = Long.MAX_VALUE
	
	protected open fun onChange(old: Long, new: Long, reason: R) {}
	
	protected open fun afterChange(old: Long, new: Long, reason: R) {}
	
	fun set(value: Long, reason: R) {
		require(value >= 0) { "Negative value" }
		
		val changed: Boolean
		val old: Long
		var new = value
		
		synchronized(lock) {
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
	}
	
	fun add(value: Long, reason: R): Long {
		if (value == 0L) return current
		
		require(value > 0) { "Negative value" }
		
		var changed: Boolean
		val old: Long
		var new: Long
		
		synchronized(lock) {
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
	
	fun take(value: Long, reason: R): Boolean {
		if (value == 0L) return true
		
		require(value > 0) { "Negative value" }
		
		val changed: Boolean
		val old: Long
		val new: Long
		
		synchronized(lock) {
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