package net.chmielowski.baggage.ui

data class EquipmentItem(
    val id: EquipmentId,
    val name: String,
    val isChecked: Boolean,
    val isDeleteVisible: Boolean,
)
