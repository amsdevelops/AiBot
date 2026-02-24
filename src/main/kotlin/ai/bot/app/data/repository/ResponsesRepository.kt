package ai.bot.app.data.repository

class ResponsesRepository {
    private val stringList = mutableListOf<String>()
    
    fun add(string: String) {
        stringList.add(string)
    }

    fun remove(index: Int) {
        if (index in stringList.indices) {
            stringList.removeAt(index)
        }
    }
    
    fun get(index: Int): String? {
        return if (index in stringList.indices) {
            stringList[index]
        } else {
            null
        }
    }
    
    fun getAll(): List<String> {
        return stringList.toList()
    }
    
    fun clear() {
        stringList.clear()
    }
    
    fun size(): Int {
        return stringList.size
    }
}