package com.au.swarin.selective_tests

import net.sourceforge.tess4j.Tesseract
import org.junit.jupiter.api.Test
import java.io.File


class ImageToTextTest {

    @Test
    fun testImageToText(){
        val image = File("/Users/amardeepkhera/Desktop/screenshots/Screenshot 2026-03-191.png")

        val tesseract = Tesseract()
        tesseract.setDatapath("/Users/amardeepkhera/Downloads/testdata")
        tesseract.setLanguage("eng")
        tesseract.setPageSegMode(1)
        tesseract.setOcrEngineMode(1)
        println(tesseract.doOCR(image))
    }
}