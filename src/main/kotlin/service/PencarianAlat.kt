package service

import model.AlatCamping

interface PencarianAlat {
    fun cariAlat(keyword: String): List<AlatCamping>
}

