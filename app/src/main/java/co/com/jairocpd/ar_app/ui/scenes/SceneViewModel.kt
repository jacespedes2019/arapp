package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.com.jairocpd.ar_app.domain.model.Nodes
import co.com.jairocpd.ar_app.domain.model.Cube
import co.com.jairocpd.ar_app.domain.model.MaterialNode
import co.com.jairocpd.ar_app.util.MaterialProperties
import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingFailureReason
import kotlin.reflect.KClass

class SceneViewModel(application: Application) : AndroidViewModel(application) {
    // Selection state
    private val _selection = MutableLiveData<KClass<out Nodes>>(Cube::class)
    val selection: LiveData<KClass<out Nodes>> = _selection

    // Node management
    private val _nodes = MutableLiveData<List<Nodes>>(emptyList())
    val nodes: LiveData<List<Nodes>> = _nodes

    // Selected node state
    private val _selectedNode = MutableLiveData<Nodes?>(null)
    val selectedNode: LiveData<Nodes?> = _selectedNode

    // Tracking state
    private val _trackingState = MutableLiveData<TrackingState?>(null)
    val trackingState: LiveData<TrackingState?> = _trackingState

    private val _trackingFailureReason = MutableLiveData<TrackingFailureReason?>(null)
    val trackingFailureReason: LiveData<TrackingFailureReason?> = _trackingFailureReason

    // Object detection state
    private val _isCubePlaced = MutableLiveData(false)
    val isCubePlaced: LiveData<Boolean> = _isCubePlaced

    // Material properties state
    private val _currentMaterialProperties = MutableLiveData(MaterialProperties.DEFAULT)
    val currentMaterialProperties: LiveData<MaterialProperties> = _currentMaterialProperties

    // Node operations
    fun addNode(node: Nodes) {
        val currentNodes = _nodes.value.orEmpty().toMutableList()
        currentNodes.add(node)
        _nodes.value = currentNodes
    }


    fun clearAllNodes() {
        _nodes.value = emptyList()
        _selectedNode.value = null
        _isCubePlaced.value = false
    }


    // Object placement state
    fun setCubePlaced(placed: Boolean) {
        _isCubePlaced.value = placed
    }

}