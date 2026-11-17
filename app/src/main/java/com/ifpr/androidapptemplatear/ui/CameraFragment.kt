package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.R
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.io.File
import kotlin.math.abs

/**
 * Exploração pela câmera/AR, descoberta de guitarras e quiz musical.
 * O gesto horizontal marca a exploração; no modo AR o enquadramento
 * real continua sendo controlado pelo movimento físico do aparelho.
 */
class CameraFragment : Fragment() {
    private lateinit var sceneView: ArSceneView
    private var modelNode: ArModelNode? = null
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var investigarButton: ExtendedFloatingActionButton
    private lateinit var textoPontos: TextView
    private lateinit var textoVidas: TextView
    private lateinit var textoExploracao: TextView
    private lateinit var sharedViewModel: SharedModelViewModel

    private val guitarras = listOf("angus_sg.glb", "ibanez_jem_guitar.glb", "jimR_jazzmaster.glb")
    private var currentModelFile = "guitar_pick.glb"
    private var descobertaAtual: String? = null
    private var distanciaGesto = 0f
    private var ultimoX: Float? = null
    private var quizAberto = false
    private val cameraPermissionCode = 1001

    private data class Pergunta(val musico: String, val correta: String, val opcoes: List<String>)
    private val perguntas = mapOf(
        "angus_sg.glb" to Pergunta("Angus Young (AC/DC)", "Thunderstruck",
            listOf("Thunderstruck", "Enter Sandman", "Smells Like Teen Spirit")),
        "ibanez_jem_guitar.glb" to Pergunta("Steve Vai", "For the Love of God",
            listOf("For the Love of God", "Highway to Hell", "Numb")),
        "jimR_jazzmaster.glb" to Pergunta("Jim Root (Slipknot)", "Psychosocial",
            listOf("Psychosocial", "Sweet Child O' Mine", "Nothing Else Matters"))
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]
        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        placeButton = view.findViewById(R.id.place)
        investigarButton = view.findViewById(R.id.investigar)
        textoPontos = view.findViewById(R.id.textPontuacao)
        textoVidas = view.findViewById(R.id.textVidas)
        textoExploracao = view.findViewById(R.id.textExploracao)

        placeButton.setOnClickListener { posicionarModelo() }
        investigarButton.setOnClickListener { abrirQuiz() }
        investigarButton.visibility = View.GONE

        // O toque não consome o evento: SceneView continua recebendo os gestos de AR.
        sceneView.setOnTouchListener { _, event ->
            if (sharedViewModel.jogoEncerrado.value == true) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> ultimoX = event.x
                MotionEvent.ACTION_MOVE -> {
                    val anterior = ultimoX
                    if (anterior != null) registrarExploracao(abs(event.x - anterior))
                    ultimoX = event.x
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> ultimoX = null
            }
            false
        }

        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { path ->
            if (!path.isNullOrBlank() && path != currentModelFile) {
                currentModelFile = path
                carregarModelo(path)
            }
        }
        sharedViewModel.pontuacao.observe(viewLifecycleOwner) {
            textoPontos.text = "Pontos: $it"
        }
        sharedViewModel.vidas.observe(viewLifecycleOwner) {
            textoVidas.text = "Vidas: $it"
        }
        sharedViewModel.jogoEncerrado.observe(viewLifecycleOwner) { encerrado ->
            if (encerrado == true) {
                investigarButton.isEnabled = false
                placeButton.isEnabled = false
            }
        }

        if (!sharedViewModel.selectedModel.value.isNullOrBlank()) {
            currentModelFile = sharedViewModel.selectedModel.value!!
        }
        verificarCamera()
        placeButton.bringToFront()
        investigarButton.bringToFront()
    }

    private fun registrarExploracao(pixels: Float) {
        if (descobertaAtual != null || sharedViewModel.jogoEncerrado.value == true) return
        distanciaGesto += pixels
        val largura = resources.displayMetrics.widthPixels.coerceAtLeast(1)
        val progresso = (distanciaGesto / (largura * 1.5f)).coerceIn(0f, 1f)
        textoExploracao.text = "Explore a câmera deslizando o dedo: ${(progresso * 100).toInt()}%"
        if (progresso >= 1f) {
            distanciaGesto = 0f
            val disponiveis = guitarras.filter {
                !sharedViewModel.respondidas.contains(it) && existeAsset(it)
            }
            if (disponiveis.isNotEmpty()) revelarGuitarra(disponiveis.random())
            else textoExploracao.text = "Todas as guitarras já foram investigadas."
        }
    }

    private fun revelarGuitarra(arquivo: String) {
        if (!existeAsset(arquivo) || sharedViewModel.respondidas.contains(arquivo)) return
        descobertaAtual = arquivo
        currentModelFile = arquivo
        sharedViewModel.selectModel(arquivo)
        carregarModelo(arquivo)
        investigarButton.visibility = View.VISIBLE
        investigarButton.isEnabled = true
        textoExploracao.text = "Guitarra encontrada! Posicione-a e toque em Investigar."
        investigarButton.bringToFront()
    }

    private fun abrirQuiz() {
        if (quizAberto || sharedViewModel.jogoEncerrado.value == true) return
        val arquivo = descobertaAtual ?: return
        val pergunta = perguntas[arquivo] ?: return
        quizAberto = true
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz: ${pergunta.musico}")
            .setMessage("Qual música foi escrita ou coescrita por este guitarrista?")
            .setCancelable(false)
            .setItems(pergunta.opcoes.toTypedArray()) { _, indice ->
                quizAberto = false
                val correta = pergunta.opcoes[indice] == pergunta.correta
                sharedViewModel.registrarResposta(arquivo, correta)
                if (correta) {
                    tocarAcerto()
                    Toast.makeText(requireContext(), "Resposta correta! +10 pontos", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Incorreto! Era: ${pergunta.correta}", Toast.LENGTH_LONG).show()
                }
                descobertaAtual = null
                investigarButton.visibility = View.GONE
                if (sharedViewModel.jogoEncerrado.value == true) {
                    textoExploracao.text = "Três erros. A tela Game Over será adicionada na Fase 4."
                } else {
                    textoExploracao.text = "Continue explorando para encontrar outra guitarra."
                }
            }
            .show()
    }

    private fun tocarAcerto() {
        // Efeito curto do sistema; não reproduz nenhum trecho de música comercial.
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            sceneView.postDelayed({ tone.release() }, 400)
        }
    }

    private fun existeAsset(arquivo: String): Boolean = runCatching {
        requireContext().assets.open(arquivo).use { }
        true
    }.getOrDefault(false)

    private fun verificarCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            carregarModelo(currentModelFile)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.CAMERA), cameraPermissionCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            carregarModelo(currentModelFile)
        } else if (requestCode == cameraPermissionCode) {
            Toast.makeText(requireContext(), "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
        }
    }

    private fun posicionarModelo() {
        if (sharedViewModel.jogoEncerrado.value == true) return
        val node = modelNode ?: return
        runCatching {
            node.anchor()
            node.isEditable = true
            node.isScaleEditable = false
            sceneView.planeRenderer.isVisible = false
        }
    }

    private fun carregarModelo(arquivo: String) {
        if (!::sceneView.isInitialized || !isAdded) return
        modelNode?.let { anterior -> runCatching { sceneView.removeChild(anterior) } }
        modelNode = null
        val local = caminhoModelo(requireContext(), arquivo) ?: return
        val node = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = local,
                centerOrigin = Position(0f, 0f, 1f)
            ) { sceneView.planeRenderer.isVisible = true }
            isEditable = true
            isScaleEditable = false
        }
        modelNode = node
        sceneView.addChild(node)
    }

    private fun caminhoModelo(context: Context, fileName: String): String? {
        if (runCatching { context.assets.open(fileName).use {} }.isSuccess) return fileName
        val file = File(context.filesDir, fileName)
        return if (file.isFile) Uri.fromFile(file).toString() else null
    }

    override fun onDestroyView() {
        modelNode = null
        super.onDestroyView()
    }
}
