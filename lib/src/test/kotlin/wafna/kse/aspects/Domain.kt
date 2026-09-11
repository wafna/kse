package wafna.kse.aspects

// There's no value in combining the pairs with inheritance, either syntactically or semantically.

internal data class UserWip(val name: String)
internal data class User(val id: Int, val name: String)
internal data class DocumentWip(val name: String, val ownerId: Int)
internal data class Document(val id: Int, val name: String, val ownerId: Int)
