package dev.benica.mileagetracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER
import com.wtb.notificationutil.NotificationUtils
import dev.benica.mileagetracker.ActivityTransitionReceiver.Companion.ACT_TRANS_INTENT
import dev.benica.mileagetracker.ActivityUpdateReceiver.Companion.ACT_UPDATE_INTENT
import dev.benica.mileagetracker.LocationService.Companion.dtf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val Any.TAG: String
    get() = "LP2_" + this.javaClass.simpleName

@ExperimentalCoroutinesApi
fun Location.toText(): String {
    val epochTime =
        Instant.ofEpochSecond(this.time / 1000).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .format(dtf)
    return "$epochTime | Lat: %.2f | Long: %.2f".format(latitude, longitude)
}

/**
 * TODO
 *
 */
@ExperimentalCoroutinesApi
class LocationService : Service() {
    // State accessors
    /**
     * The current [ServiceState] of the [LocationService]
     */
    val serviceState: StateFlow<ServiceState>
        get() = _serviceState

    /**
     * The current tripId, null if not set. It is the value that is passed to the handler that is
     * passed to [start].
     */
    val tripId: StateFlow<Long?>
        get() = _tripId

    /**
     * The user's detected activity is [inVehicle] if [isTesting] is false, [onFoot] if it is true
     */
    val inSelectedTransport: StateFlow<Boolean>
        get() = _inSelectedTransport

    /**
     * The service is running
     */
    val isStarted: StateFlow<Boolean>
        get() = _isStarted

    /**
     * The user's detected activity is [DetectedActivity.STILL]
     */
    val isStill: StateFlow<Boolean>
        get() = _isStill

    /**
     * The user's detected activity is [DetectedActivity.IN_VEHICLE]
     */
    val inVehicle: StateFlow<Boolean>
        get() = _inVehicle

    /**
     *  The user's detected activity is [DetectedActivity.ON_FOOT]
     */
    val onFoot: StateFlow<Boolean>
        get() = _onFoot

    /**
     * The service is in testing mode. If true, location updates are always processed, else only
     * when ![isStill] and [inVehicle]
     */
    val isTesting: StateFlow<Boolean>
        get() = _isTesting

    fun setIsTesting(isTesting: Boolean) {
        _isTesting.value = isTesting
    }

    /**
     * Location updates are being processed. [serviceState] is [ServiceState.TRACKING] and either
     * [isTesting] or ![isStill] and [inVehicle]
     */
    val isTracking: StateFlow<Boolean>
        get() = _isTracking

    // Public methods
    /**
     * Initializes the service with information for creating the ongoing notification that shows
     * when the service is running in the background (as a foreground service)
     *
     * @param notificationData a [NotificationUtils.NotificationData] for creating the ongoing
     * [Notification]
     * @param notificationChannel the [NotificationChannel] of the [Notification]
     * @param notificationText a function to get the initial main text of the [Notification] from
     * the current [Location]. see [Notification.Builder.setContentText]
     * @param updateNotificationText null if the [Notification] text should not update when a new
     * location is received; otherwise a function to process a [Location]
     */
    fun initialize(
        notificationData: NotificationUtils.NotificationData,
        notificationChannel: NotificationChannel,
        notificationText: (Location?) -> String,
        updateNotificationText: ((Location?) -> String)? = null
    ) {
        this.nd = notificationData
        this.notificationChannel = notificationChannel
        this.getNotificationText = notificationText
        this.getUpdateNotificationText = updateNotificationText
    }

    /**
     * Starts location tracking. Sets [serviceState] to [ServiceState.TRACKING]. It will call
     * [Service.startService] if necessary, and will begin receiving [Location] updates.
     * If [serviceState] was [ServiceState.STOPPED], it sets [tripId] to [newTripId]. If
     * [serviceState] was [ServiceState.PAUSED] or [ServiceState.TRACKING], [tripId] remains
     * unchanged.
     *
     * @param newTripId the next expected [tripId]
     * @param locationHandler a handler function for incoming [Location] and the current [tripId]
     * @return if the [serviceState] was originally [ServiceState.STOPPED], [newTripId]. If the
     * [serviceState] was [ServiceState.PAUSED] or [ServiceState.TRACKING], it will return the
     * existing [tripId].
     */
    fun start(newTripId: Long, locationHandler: (Location, Long) -> Unit): Long {
        when (serviceState.value) {
            ServiceState.STOPPED -> {
                _tripId.value = newTripId
                _isStarted.value = true

                startService(Intent(applicationContext, LocationService::class.java))

                registerReceiver(activityUpdateReceiver, IntentFilter(ACT_UPDATE_INTENT))
                userActivity.registerForActivityUpdates()
                activityUpdateReceiverRegistered = true

                registerReceiver(activityTransitionReceiver, IntentFilter(ACT_TRANS_INTENT))
                userActivity.registerForActivityTransitionUpdates()
                activityTransitionReceiverRegistered = true

                try {
                    locationCallback = getLocationCallback(locationHandler)
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback!!,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, e.toString())
                }
            }
            ServiceState.PAUSED -> {
                _isStarted.value = true
            }
            else -> {
                // Do nothing
            }
        }

        return this.tripId.value!!
    }

    /**
     * Pauses [Location] updates, which changes [serviceState] to [ServiceState.PAUSED]. If
     * [start] is called again before a call to [stop], [Location] updates will resume using the
     * same [tripId].
     *
     */
    fun pause() {
        _isStarted.value = false
    }

    /**
     * Stops [Location] updates and sets [tripId] to null. The next time [start] is called,
     * [tripId] will be set to the 'newTripId' argument.
     *
     */
    fun stop() {
        userActivity.removeActivityTransitionUpdates()
        if (activityTransitionReceiverRegistered) {
            unregisterReceiver(activityTransitionReceiver)
            activityTransitionReceiverRegistered = false
        }

        if (activityUpdateReceiverRegistered) {
            unregisterReceiver(activityUpdateReceiver)
            activityUpdateReceiverRegistered = false
        }

        try {
            val removeTask = locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

            if (removeTask != null) {
                removeTask.addOnCompleteListener {
                    if (it.isSuccessful) {
                        _tripId.value = null
                        _isStarted.value = false
                        stopSelf()
                    }
                }
            } else {
                _tripId.value = null
                _isStarted.value = false
                stopSelf()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.toString())
        }
    }

    // Private properties
    private var serviceRunningInForeground = false
    private var configurationChange = false
    private var activityTransitionReceiverRegistered = false
    private var activityUpdateReceiverRegistered = false

    private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
    private val _tripId = MutableStateFlow<Long?>(null)
    private val _inSelectedTransport = MutableStateFlow(false)
    private val _isStarted = MutableStateFlow(false)
    private val _isStill = MutableStateFlow(false)
    private val _inVehicle = MutableStateFlow(false)
    private val _onFoot = MutableStateFlow(false)
    private val _isTesting = MutableStateFlow(false)
    private val _isTracking = MutableStateFlow(false)

    private val localBinder = LocalBinder()

    // Notification stuff
    private var nd: NotificationUtils.NotificationData? = null
    private var notificationChannel: NotificationChannel? = null
    private val notificationUtil by lazy {
        NotificationUtils(
            context = this,
            nd = nd ?: throw UninitializedPropertyAccessException(
                getString(
                    R.string.uninit_notif_data_exception
                )
            )
        )
    }
    private var getNotificationText: ((Location?) -> String)? = null
    var getUpdateNotificationText: ((Location?) -> String)? = null

    // Location tracking
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null

    // Activity Detection
    private val userActivity = ActivityTransitionUtils(this)

    private fun getLocationCallback(onComplete: (Location, Long) -> Unit) =
        object : LocationCallback() {
            override fun onLocationResult(loc: LocationResult) {
                super.onLocationResult(loc)
                val lastLoc = loc.lastLocation
                if (lastLoc == null || !lastLoc.hasAccuracy() || lastLoc.accuracy > 20f || !isTracking.value) {
                    return
                }

                currentLocation = lastLoc
                tripId.value?.let { onComplete(lastLoc, it) }

                if (serviceRunningInForeground) {
                    getUpdateNotificationText?.let {
                        notificationUtil.updateNotification(
                            it(currentLocation),
                            NOTIFICATION_ID,
                            notificationChannel
                                ?: throw IllegalStateException(getString(R.string.uninit_notif_data_exception))

                        )
                    }
                }
            }
        }

    override fun onCreate() {
        initObservers()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if (activityUpdateReceiverRegistered)
            unregisterReceiver(activityUpdateReceiver)

        if (activityTransitionReceiverRegistered)
            unregisterReceiver(activityTransitionReceiver)
    }

    override fun onBind(intent: Intent): IBinder {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!configurationChange) {
            val notification: Notification = notificationUtil.generateNotification(
                getNotificationText?.invoke(currentLocation) ?: "Location Service in use.",
                notificationChannel
                    ?: throw IllegalStateException(getString(R.string.uninit_notif_data_exception))
            )
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private val activityUpdateReceiver = ActivityUpdateReceiver().apply {
        action = { result ->
            val still = result.getActivityConfidence(DetectedActivity.STILL)
            val car = result.getActivityConfidence(DetectedActivity.IN_VEHICLE)
            val foot = result.getActivityConfidence(DetectedActivity.ON_FOOT)
            val unknown = result.getActivityConfidence(DetectedActivity.UNKNOWN)

            if (unknown < 20) {
                _isStill.value = still > 50
                _inVehicle.value = car > 50
                _onFoot.value = foot > 50
            }

            if (unknown < 10 && (car > 50 || foot > 50 || still > 90)) {
                unregisterReceiver(this)
                activityUpdateReceiverRegistered = false
                userActivity.removeActivityUpdates()
            }
        }
    }

    private val activityTransitionReceiver = ActivityTransitionReceiver().apply {
        action = { event ->
            when (event.activityType) {
                DetectedActivity.STILL -> {
                    _isStill.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                DetectedActivity.IN_VEHICLE -> {
                    _inVehicle.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                DetectedActivity.ON_FOOT -> {
                    _onFoot.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun initObservers() {
        fun updateTrack() {
            _isTracking.value =
                isStarted.value && (isTesting.value || (!isStill.value && inVehicle.value))
        }

        fun updateServiceState() {
            _serviceState.value = when {
                isStarted.value && (tripId.value != null) -> ServiceState.TRACKING
                !isStarted.value && (tripId.value != null) -> ServiceState.PAUSED
                else -> ServiceState.STOPPED
            }
        }

        fun updateMode() {
            _inSelectedTransport.value = if (isTesting.value) onFoot.value else inVehicle.value
        }

        CoroutineScope(Dispatchers.Default).launch {
            tripId.collectLatest {
                updateServiceState()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            isTesting.collectLatest {
                updateMode()
                updateTrack()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            isStill.collectLatest {
                updateTrack()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            inVehicle.collectLatest {
                updateMode()
                updateTrack()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            onFoot.collectLatest {
                updateMode()
                updateTrack()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            isStarted.collectLatest {
                updateTrack()
                updateServiceState()
            }
        }
    }

    inner class LocalBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    enum class ServiceState {
        TRACKING, PAUSED, STOPPED
    }

    companion object {
        private const val NOTIFICATION_ID = 101101
        private const val UPDATE_INTERVAL_MILLISECONDS: Long = 5000
        private const val FASTEST_UPDATE_INTERVAL_MILLISECONDS = UPDATE_INTERVAL_MILLISECONDS / 2

        private val locationRequest: LocationRequest
            get() = LocationRequest.create().apply {
                interval = UPDATE_INTERVAL_MILLISECONDS
                fastestInterval = FASTEST_UPDATE_INTERVAL_MILLISECONDS
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

        var dtf: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")

        var df: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    }
}

