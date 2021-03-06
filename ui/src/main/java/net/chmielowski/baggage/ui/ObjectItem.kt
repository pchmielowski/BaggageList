package net.chmielowski.baggage.ui

data class ObjectItem(
    val id: ObjectId,
    val name: String,
    val isChecked: Boolean,
    val isDeleteVisible: Boolean,
)
