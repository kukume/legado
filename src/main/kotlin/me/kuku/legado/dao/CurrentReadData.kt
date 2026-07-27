package me.kuku.legado.dao

import me.kuku.legado.api.dto.BookChapterDTO
import me.kuku.legado.api.dto.BookDTO

object CurrentReadData {

    @JvmStatic
    var book: BookDTO = BookDTO()

    @JvmStatic
    var bookChapterList: List<BookChapterDTO> = listOf()

    @JvmStatic
    var bookIndex: Int = -1

    @JvmStatic
    var bodyContent: String = ""

    @JvmStatic
    fun getBookChapter(): BookChapterDTO {
        return bookChapterList[bookIndex]
    }

    fun indexAtomicIncrement() {
        bookIndex++
    }

    fun indexAtomicDecrement() {
        bookIndex--
    }

}
