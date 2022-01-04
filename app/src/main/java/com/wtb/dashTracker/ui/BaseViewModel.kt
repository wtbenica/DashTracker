package com.wtb.dashTracker.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class BaseViewModel<T: DataModel>: ViewModel() {
    protected val repository = Repository.get()

    private val _id = MutableStateFlow(AUTO_ID)
    protected val id: StateFlow<Int>
        get() = _id

    internal val item: StateFlow<T?> = id.flatMapLatest { id ->
        Log.d(TAG, "This is id: $id")
        val itemFlow = getItemFlowById(id)
        itemFlow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    abstract fun getItemFlowById(id: Int): Flow<T?>

    fun loadDataModel(id: Int?) {
        _id.value = id ?: AUTO_ID
    }

    fun insert(dataModel: DataModel) = repository.saveModel(dataModel)

    fun upsert(dataModel: DataModel) {
        CoroutineScope(Dispatchers.Default).launch {
            val id = repository.upsertModel(dataModel)
            if (id != -1L) {
                _id.value = id.toInt()
            }
        }
    }

    suspend fun upsertSus(dataModel: DataModel) = repository.upsertModel(dataModel)

    fun delete(dataModel: DataModel) = repository.deleteModel(dataModel)

    fun clearEntry() {
        _id.value = AUTO_ID
    }

    companion object {
        private const val TAG = APP + "BaseViewModel"
    }
}