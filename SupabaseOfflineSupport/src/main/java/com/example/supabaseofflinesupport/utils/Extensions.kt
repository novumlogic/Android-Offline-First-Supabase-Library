package com.example.supabaseofflinesupport.utils

import android.util.Log
import com.example.supabaseofflinesupport.BaseRemoteEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Decodes a JSON string into a single RemoteEntity object.*
 * @param serializer The Kotlinx serialization serializer for the RemoteEntity type.
 * @return The decoded RemoteEntity object.
 */
fun <RDto : BaseRemoteEntity> String.decodeSingle(serializer: KSerializer<RDto>): RDto {
    return Json.decodeFromString(ListSerializer(serializer), this).first()
}


/**
 * Decodes a JSON string into a list of RemoteEntity objects.
 *
 * @param serializer The Kotlinx serialization serializer for the RemoteEntity type.
 * @return The decoded list of RemoteEntity objects.
 */
fun <RDto : BaseRemoteEntity> String.decodeList(serializer: KSerializer<RDto>): List<RDto> {
    return Json.decodeFromString(ListSerializer(serializer), this)
}

/**
 * Extracts the ID from a successful Response object.
 *
 * @return The ID extracted from the response.
 * @throws Exception If the response body is null or cannot be parsed.
 */
fun Response<ResponseBody>.getId(): Int {
    val element = Json.parseToJsonElement(this.body()!!.string())
    return if (element is JsonArray)
        Json.decodeFromJsonElement<List<JsonId>>(element).first().id
    else {
        val error = Json.decodeFromJsonElement<JsonError>(element)
        throw Exception("code: ${error.code}, hint: ${error.hint}, details: ${error.details}, message: ${error.message}")
    }
}

/**
 * Data class to represent a JSON object containing an ID.
 */
@Serializable
data class JsonId(val id: Int)

/**
 * Data class to represent a JSON error object.
 */
@Serializable
data class JsonError(
    val code: String?,
    val details: String?,
    val hint: String?,
    val message: String?
)

/**
 * Prepares a RequestBody object from a RemoteEntity object, including the ID.
 *
 * @param serializer The Kotlinx serialization serializer for the RemoteEntity type.
 * @return The prepared RequestBody object.
 */
fun <T : BaseRemoteEntity> T.prepareRequestBody(serializer: KSerializer<T>): RequestBody {
    val jsonString = Json.encodeToString(serializer, this)

    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    return jsonString.toRequestBody(jsonMediaType)
}

/**
 * Prepares a RequestBody object from a RemoteEntity object, excluding the ID.
 *
 * @param serializer The Kotlinx serialization serializer for the RemoteEntity type.
 * @return The prepared RequestBody object without the ID.
 */
fun <T : BaseRemoteEntity> T.prepareRequestBodyWithoutId(serializer: KSerializer<T>): RequestBody {
    val jsonString = Json.encodeToString(serializer, this)
    val modifiedJson = removeIdFromJson(jsonString)
    Log.d("Utils", "prepareRequestBodyWithoutId: original $jsonString and modified $modifiedJson ")
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    return modifiedJson.toRequestBody(jsonMediaType)
}

/**
 * Removes the "id" field from a JSON string.
 *
 * @param str The JSON string.
 * @return The modified JSON string without the "id" field.
 */
fun removeIdFromJson(str: String): String {
    val original = Json.parseToJsonElement(str).jsonObject
    val modifiedObj = buildJsonObject {
        original.entries.forEach { (key, value) ->
            if (key != "id")
                put(key, value)
        }
    }
    return Json.encodeToString(JsonObject.serializer(), modifiedObj)
}
