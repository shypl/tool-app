package org.shypl.tool.app.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import java.time.Duration

private val regex = Regex("\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s*")

private fun stringToDuration(string: String): Duration {
	val e = regex.matchEntire(string)
	if (e != null) {
		var v = Duration.ZERO
		e.groups[1]?.also { v = v.plusDays(it.value.toLong()) }
		e.groups[2]?.also { v = v.plusHours(it.value.toLong()) }
		e.groups[3]?.also { v = v.plusMinutes(it.value.toLong()) }
		e.groups[4]?.also { v = v.plusSeconds(it.value.toLong()) }
		return v
	}
	throw IllegalArgumentException()
}

class DurationDeserializer : JsonDeserializer<Duration>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Duration {
		return stringToDuration(p.valueAsString)
	}
}

class TimeStringToSecondsIntDeserializationProblemHandler : DeserializationProblemHandler() {
	
	override fun handleWeirdStringValue(ctxt: DeserializationContext, targetType: Class<*>, valueToConvert: String, failureMsg: String): Any {
		if (targetType == Int::class.java || targetType == Long::class.java) {
			val duration = stringToDuration(valueToConvert).seconds
			if (targetType == Int::class.java) {
				return duration.toInt()
			}
			return duration
		}
		
		return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg)
	}
}