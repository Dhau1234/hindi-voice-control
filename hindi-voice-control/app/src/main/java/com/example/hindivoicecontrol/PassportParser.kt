package com.example.hindivoicecontrol

class PassportParser {

    fun extract(text: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val passportNo = Regex("(?i)p(?:assport)?[^0-9]{0,15}(\\d{6,12})").find(text)?.groupValues?.get(1)
        if (passportNo != null) fields["passport_no"] = passportNo

        val name = Regex("(?i)name[:\\s]*([A-Za-z\\s]{3,60})").find(text)?.groupValues?.get(1)?.trim()
        if (name != null) fields["name"] = name

        val dob = Regex("(?i)date\\s*of\\s*birth[:\\s]*([0-9]{2}[/-][0-9]{2}[/-][0-9]{4})").find(text)?.groupValues?.get(1)
        if (dob != null) fields["dob"] = dob

        val nationality = Regex("(?i)nationality[:\\s]*([A-Za-z\\s]{2,30})").find(text)?.groupValues?.get(1)?.trim()
        if (nationality != null) fields["nationality"] = nationality

        return fields
    }
}
