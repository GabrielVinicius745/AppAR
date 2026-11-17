package com.ifpr.androidapptemplatear.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedModelViewModel : ViewModel() {
    val selectedModel = MutableLiveData<String>()
    private val _pontuacao = MutableLiveData(0)
    val pontuacao: LiveData<Int> = _pontuacao
    private val _vidas = MutableLiveData(3)
    val vidas: LiveData<Int> = _vidas
    private val _jogoEncerrado = MutableLiveData(false)
    val jogoEncerrado: LiveData<Boolean> = _jogoEncerrado

    private val modelosEncontrados = mutableSetOf<String>()
    val respondidas = mutableSetOf<String>()

    fun selectModel(modelPath: String) { selectedModel.value = modelPath }

    /** Apenas o callback de GPS deve registrar um encontro geográfico. */
    fun registrarEncontro(arquivo: String) {
        if (_jogoEncerrado.value == true) return
        if (modelosEncontrados.add(arquivo)) {
            _pontuacao.value = (_pontuacao.value ?: 0) + 10
        }
    }

    /** Cada guitarra responde apenas uma vez durante a partida. */
    fun registrarResposta(arquivo: String, correta: Boolean) {
        if (_jogoEncerrado.value == true || !respondidas.add(arquivo)) return
        if (correta) {
            _pontuacao.value = (_pontuacao.value ?: 0) + 10
        } else {
            val restantes = ((_vidas.value ?: 3) - 1).coerceAtLeast(0)
            _vidas.value = restantes
            if (restantes == 0) _jogoEncerrado.value = true
        }
    }

    fun reiniciarPontuacao() {
        modelosEncontrados.clear()
        respondidas.clear()
        _pontuacao.value = 0
        _vidas.value = 3
        _jogoEncerrado.value = false
        selectedModel.value = null
    }
}
