package net.chmielowski.baggage.`object`

data class ObjectItem(
    val id: ObjectId,
    val name: String,
    val isChecked: Boolean,
    val isDeleteVisible: Boolean,
)
