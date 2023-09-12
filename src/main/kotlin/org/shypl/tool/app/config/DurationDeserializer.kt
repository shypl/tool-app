package org.shypl.tool.app.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import java.time.Duration

private val regex = Regex("\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s*")

private fun stringToSeconds(string: String): Int {
	val e = regex.matchEntire(string)
	if (e != null) {
		var v = 0
		e.groups[1]?.also { v += it.value.toInt() * 60 * 60 * 24 }
		e.groups[2]?.also { v += it.value.toInt() * 60 * 60 }
		e.groups[3]?.also { v += it.value.toInt() * 60 }
		e.groups[4]?.also { v += it.value.toInt() }
		return v
	}
	throw IllegalArgumentException()
}

class DurationDeserializer : JsonDeserializer<Duration>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Duration {
		return Duration.ofSeconds(stringToSeconds(p.valueAsString).toLong())
	}
}

class TimeStringToSecondsIntDeserializationProblemHandler : DeserializationProblemHandler() {
	
	override fun handleWeirdStringValue(ctxt: DeserializationContext, targetType: Class<*>, valueToConvert: String, failureMsg: String): Any {
		if (targetType == Int::class.java || targetType == Long::class.java) {
			return stringToSeconds(valueToConvert)
		}
		
		return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg)
	}
}