package org.fossasia.openevent.general.attendees

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections

class ListAttendeeIdConverter {
    var gson = Gson()
    @TypeConverter
    fun stringToSomeObjectList(data: String?): List<AttendeeId> {
        if (data == null) {
            return Collections.emptyList()
        }
        val listType = object : TypeToken<List<AttendeeId>>() {}.type

        return gson.fromJson<List<AttendeeId>>(data, listType)
    }

    @TypeConverter
    fun someObjectListToString(someObjects: List<AttendeeId>): String {
        return gson.toJson(someObjects)
    }
}
